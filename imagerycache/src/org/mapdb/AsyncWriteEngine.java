/*
 *  Copyright (c) 2012 Jan Kotek
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.mapdb;

import java.lang.ref.WeakReference;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.*;

/**
 * {@link Engine} wrapper which provides asynchronous serialization and write.
 *  This class takes an object instance, passes it to background thread (using Queue)
 *  where it is serialized and written to disk.
 * <p/>
 * Async write does not affect commit durability, write queue is flushed before each commit.
 *
 * @author Jan Kotek
 */
public class AsyncWriteEngine extends EngineWrapper implements Engine {

    protected static final AtomicLong threadCounter = new AtomicLong();
    protected final long threadNum = threadCounter.incrementAndGet();

    protected final BlockingQueue<Long> newRecids = new ArrayBlockingQueue<Long>(128);

    protected volatile boolean closeInProgress = false;
    protected final CountDownLatch shutdownCondition = new CountDownLatch(2);
    protected final int asyncFlushDelay;

    protected static final Object DELETED = new Object();
    protected final ReentrantLock[] writeLocks = Utils.newLocks(32);

    protected final ReentrantReadWriteLock commitLock;

    protected Throwable writerFailedException = null;

    protected final LongConcurrentHashMap<Fun.Tuple2<Object,Serializer<Object>>> items = new LongConcurrentHashMap<>();

    protected final Thread newRecidsThread = new Thread("MapDB prealloc #"+threadNum){
        @Override public void run() {
            try{
                for(;;){
                    if(closeInProgress || (parentEngineWeakRef!=null && parentEngineWeakRef.get()==null) || writerFailedException!=null) return;
                    Long newRecid = getWrappedEngine().put(Utils.EMPTY_STRING, Serializer.EMPTY_SERIALIZER);
                    newRecids.put(newRecid);
                }
            } catch (Throwable e) {
                writerFailedException = e;
            }finally {
                shutdownCondition.countDown();
            }
        }
    };

    protected final Thread writerThread = new Thread("MapDB writer #"+threadNum){
        @Override public void run() {
            try{

                for(;;){
                    LongMap.LongMapIterator<Fun.Tuple2<Object,Serializer<Object>>> iter = items.longMapIterator();

                    if(!iter.moveToNext()){
                        //empty map, pause for a moment to give it chance to fill
                        if( (parentEngineWeakRef!=null && parentEngineWeakRef.get()==null) || writerFailedException!=null) return;
                        Thread.sleep(asyncFlushDelay);
                        if(closeInProgress){
                            //lock world and write everything
                            Utils.lockAll(writeLocks);
                            try{
                                while(!items.isEmpty()){
                                    iter = items.longMapIterator();
                                    while(iter.moveToNext()){
                                        long recid = iter.key();
                                        Fun.Tuple2<Object,Serializer<Object>> value = iter.value();
                                        if(value.a==DELETED){
                                            AsyncWriteEngine.super.delete(recid, value.b);
                                        }else{
                                            AsyncWriteEngine.super.update(recid, value.a, value.b);
                                        }
                                        items.remove(recid, value);
                                    }
                                }
                                return;
                            }finally{
                                Utils.unlockAll(writeLocks);
                            }
                        }
                    }else do{
                        //iterate over items and write them
                        long recid = iter.key();

                        Utils.lock(writeLocks,recid);
                        try{
                            Fun.Tuple2<Object,Serializer<Object>> value = iter.value();
                            if(value.a==DELETED){
                                AsyncWriteEngine.super.delete(recid, value.b);
                            }else{
                                AsyncWriteEngine.super.update(recid, value.a, value.b);
                            }
                            items.remove(recid, value);
                        }finally {
                            Utils.unlock(writeLocks, recid);
                        }
                    }while(iter.moveToNext());

                }
            } catch (Throwable e) {
                writerFailedException = e;
            }finally {
                shutdownCondition.countDown();
            }
        }
    };



    protected AsyncWriteEngine(Engine engine, boolean _transactionsDisabled, boolean _powerSavingMode, int _asyncFlushDelay) {
        super(engine);

        newRecidsThread.setDaemon(true);
        writerThread.setDaemon(true);

        commitLock = _transactionsDisabled? null: new ReentrantReadWriteLock();
        newRecidsThread.start();
        writerThread.start();
        asyncFlushDelay = _asyncFlushDelay;

    }

    @Override
    public <A> long put(A value, Serializer<A> serializer) {
        if(commitLock!=null) commitLock.readLock().lock();
        try{
            try {
                Long recid = newRecids.take(); //TODO possible deadlock while closing
                update(recid, value, serializer);
                return recid;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }finally{
            if(commitLock!=null) commitLock.readLock().unlock();
        }

    }

    protected void checkState() {
        if(closeInProgress) throw new IllegalAccessError("db has been closed");
        if(writerFailedException!=null) throw new RuntimeException("Writer thread failed", writerFailedException);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> A get(long recid, Serializer<A> serializer) {
        if(commitLock!=null) commitLock.readLock().lock();
        try{
            Utils.lock(writeLocks,recid);
            try{
                checkState();
                Fun.Tuple2<Object,Serializer<Object>> item = items.get(recid);
                if(item!=null){
                    if(item.a == DELETED) return null;
                    return (A) item.a;
                }

                return super.get(recid, serializer);
            }finally{
                Utils.unlock(writeLocks,recid);
            }
        }finally{
            if(commitLock!=null) commitLock.readLock().unlock();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <A> void update(long recid, A value, Serializer<A> serializer) {

        if(commitLock!=null && serializer!=SerializerPojo.serializer) commitLock.readLock().lock();
        try{

            Utils.lock(writeLocks, recid);
            try{
                checkState();
                items.put(recid, new Fun.Tuple2(value,serializer));
            }finally{
                Utils.unlock(writeLocks, recid);
            }
        }finally{
            if(commitLock!=null&& serializer!=SerializerPojo.serializer) commitLock.readLock().unlock();
        }

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <A> boolean compareAndSwap(long recid, A expectedOldValue, A newValue, Serializer<A> serializer) {
        //TODO commit lock?
        Utils.lock(writeLocks, recid);
        try{
            checkState();
            Fun.Tuple2<Object, Serializer<Object>> existing = items.get(recid);
            A oldValue = existing!=null? (A) existing.a : super.get(recid, serializer);
            if(oldValue == expectedOldValue || (oldValue!=null && oldValue.equals(expectedOldValue))){
                items.put(recid, new Fun.Tuple2(newValue,serializer));
                return true;
            }else{
                return false;
            }
        }finally{
            Utils.unlock(writeLocks, recid);

        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> void delete(long recid, Serializer<A> serializer) {
        update(recid, (A) DELETED, serializer);
    }

    @Override
    public void close() {
        try {
            if(closeInProgress) return;
            closeInProgress = true;
            //put preallocated recids back to store
            for(Long recid = newRecids.poll(); recid!=null; recid = newRecids.poll()){
                super.delete(recid, Serializer.EMPTY_SERIALIZER);
            }
            //TODO commit after returning recids?

            //wait for worker threads to shutdown
            shutdownCondition.await();

            super.close();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected WeakReference<Engine> parentEngineWeakRef = null;

    /**
     * Main thread may die, leaving Writer Thread orphaned.
     * To prevent this we periodically check if WeakReference was GCed.
     * This method sets WeakReference to user facing Engine,
     * if this instance if GCed it means that user may no longer manage
     * and we can exit Writer Thread.
     *
     * @param parentEngineReference reference to user facing Engine
     */
    public void setParentEngineReference(Engine parentEngineReference) {
        parentEngineWeakRef = new WeakReference<Engine>(parentEngineReference);
    }

    @Override
    public void commit() {
        checkState();
        if(commitLock==null){
            super.commit();
            return;
        }
        commitLock.writeLock().lock();
        try{
            while(!items.isEmpty()) {
                checkState();
                LockSupport.parkNanos(100);
            }

            super.commit();
        }finally {
            commitLock.writeLock().unlock();
        }
    }

    @Override
    public void rollback() {
        checkState();
        if(commitLock == null) throw new UnsupportedOperationException("transactions disabled");
        commitLock.writeLock().lock();
        try{
            while(!items.isEmpty()) LockSupport.parkNanos(100);
            newRecids.clear();
            super.rollback();
        }finally {
            commitLock.writeLock().unlock();
        }
    }
}
