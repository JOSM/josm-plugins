package org.openstreetmap.josm.plugins.videomapping.video;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.text.DateFormat;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.gui.util.GuiHelper;

import uk.co.caprica.vlcj.player.DeinterlaceMode;

/** basic UI of a videoplayer for multiple videos incl. notifications
 */
public class VideoPlayer extends JFrame implements WindowListener, VideosObserver, VideoPlayerObserver {
    private static final int notificationIntervall = 500;
    protected JPanel screenPanel,controlsPanel,canvasPanel;
    private JSlider timeline;
    private JButton play,back,forward;
    private JToggleButton loop,mute;
    private JSlider speed;
//    private DateFormat videoTimeFormat;
    private VideoEngine videoengine;
    private long jumpLength;
    private long loopLength;
    private Timer loopingTimer;
    private boolean isManualJump;
    private Timer notificationTimer;
    private List<VideoPlayerObserver> observers;
    
    public VideoPlayer(DateFormat videoTimeFormat) throws HeadlessException {
        super();        
//        this.videoTimeFormat=videoTimeFormat;
        //setup playback notifications
        videoengine=new VideoEngine(this);
        videoengine.addObserver(this);
        observers=new LinkedList<VideoPlayerObserver>();        
        addObserver(this);
        //setup GUI
        setSize(400, 300); //later we apply movie size
        setAlwaysOnTop(true);
        createUI();
        addUI();
        addUIListeners();    
        setVisible(true);
        setAlwaysOnTop(true);        
        this.addWindowListener(this);
    }
    
    public Video addVideo(File videofile, String id) {
        Video video = new Video(videofile,id,videoengine.mediaPlayerFactory);
        canvasPanel.add(video.panel);
        video.canvas.setSize(new Dimension(300, 300)); // will be updated by the video engine itself
        videoengine.add(video);
        pack();
        startNotificationTimer();
        return video;
    }
    
    public List <Video> getVideos() {
        return videoengine.getVideos();
    }
    
    public void pause() {
        videoengine.pause();
        if (videoengine.isNoVideoPlaying())
            stopNotificationTimer();
        else
            startNotificationTimer();
    }
    
    public void pauseAll() {
        stopNotificationTimer();
        videoengine.pauseAll();
    }

    public void backward() {
        videoengine.jumpFor(-jumpLength);    
    }

    public void forward() {
        videoengine.jumpFor(jumpLength);    
    }

    public void setSpeed(Integer percent) {
        speed.setValue(percent);        
    }
    
    public Integer getSpeed() {
        return speed.getValue();
    }
    
    public void setDeinterlacer(DeinterlaceMode deinterlacer) {
        videoengine.setDeinterlacer(deinterlacer);
    }
    
    public void setSubtitles(boolean enabled) {
        videoengine.setSubtitles(enabled);
    }

    public void mute() {
        videoengine.mute();
    }

    //TODO auf mehrere Videos umstellen
    public void toggleLooping() {
        if (loopingTimer==null) {
            //do reset after loop time experienced
            final long videoResetTime = (long) videoengine.getVideoTime()-loopLength/2;
            TimerTask reset = new TimerTask() {                
                @Override
                public void run() {
                    videoengine.jumpTo(videoResetTime);
                }
            };
            loopingTimer= new Timer();
            loopingTimer.schedule(reset,loopLength/2,loopLength);
        } else {
            loopingTimer.cancel();
            loopingTimer=null;
        }
    }

    //create all normal player controls
    private void createUI() {
        //setIconImage();
        timeline = new JSlider(0,100,0);
        timeline.setMajorTickSpacing(5);
        timeline.setMinorTickSpacing(1);
        timeline.setPaintTicks(true);
        play= new JButton(tr("play"));
        back= new JButton("<");
        forward= new JButton(">");
        loop= new JToggleButton(tr("loop"));
        mute= new JToggleButton(tr("mute"));
        speed = new JSlider(0,200,100);
        speed.setMajorTickSpacing(50);
        speed.setPaintTicks(true);          
        speed.setOrientation(Adjustable.VERTICAL);
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put( new Integer( 100 ), new JLabel("1x") );
        labelTable.put( new Integer( 50 ), new JLabel("-2x") );
        labelTable.put( new Integer( 200 ), new JLabel("2x") );
        speed.setLabelTable( labelTable );
        speed.setPaintLabels(true);
    }
    
    //puts all player controls to screen
    private void addUI() {
        //create layouts
        this.setLayout(new BorderLayout());
        screenPanel=new JPanel();
        screenPanel.setLayout(new BorderLayout());
        controlsPanel=new JPanel();
        controlsPanel.setLayout(new FlowLayout());
        canvasPanel=new JPanel();
        canvasPanel.setLayout(new FlowLayout());
        add(screenPanel,BorderLayout.CENTER);
        add(controlsPanel,BorderLayout.SOUTH);
        //fill screen panel
        screenPanel.add(canvasPanel,BorderLayout.CENTER);
        screenPanel.add(timeline,BorderLayout.SOUTH);
        screenPanel.add(speed,BorderLayout.EAST);
        controlsPanel.add(play);
        controlsPanel.add(back);
        controlsPanel.add(forward);
        controlsPanel.add(loop);
        controlsPanel.add(mute);
        loop.setSelected(false);
        mute.setSelected(false);
    }
    
    //add UI functionality
    private void addUIListeners() {        
        
        play.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent arg0) {
                pause();                
            }
        });
        
        back.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                backward();
            }
        });
        
        forward.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                forward();
            }
        });
        
        loop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                toggleLooping();
            }
        });
        
        mute.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                mute();
            }
        });
        
        timeline.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                //skip events, fired by this sliede, one cycle ago                
                if (!isManualJump) {
                    isManualJump = true;
                    videoengine.jumpToPosition((timeline.getValue()));
                }
            }
        });
        
        speed.addChangeListener(new ChangeListener() {            
            @Override
            public void stateChanged(ChangeEvent arg0) {                            
                if(!speed.getValueIsAdjusting()) {
                    videoengine.setSpeed(speed.getValue());
                }
            }
        });
    }
    
    public void setJumpLength(long ms) {
        jumpLength = ms;
    }
    
    public void setLoopLength(long ms) {
        loopLength = ms;
    }
    
    public void enableSingleVideoMode(boolean enabled) {
        pauseAll();
        videoengine.enableSingleVideoMode(enabled);
    }
    
    public void addObserver(VideoPlayerObserver observer) {
        observers.add(observer);
    }
    
    private void stopNotificationTimer() {
        /*
        if(notificationTimer!=null)
        {
            notificationTimer.cancel();
            notificationTimer=null;
        }
        */
    }

    private void startNotificationTimer() {
        notificationTimer= new Timer();
        notificationTimer.schedule(new TimerTask() {                
            @Override
            public void run() {
                notifyObservers();
                
            }
        },notificationIntervall,notificationIntervall);
    }
    
    private void  notifyObservers() {
        for (VideoPlayerObserver observer : observers) {
            observer.update_plays();//TODO hier müssten gleich die Zeiten übergeben werden
        }
    }

    @Override
    public void windowActivated(WindowEvent arg0) { }

    @Override
    public void windowClosed(WindowEvent arg0) { }

    @Override
    public void windowClosing(WindowEvent arg0) {    
        videoengine.unload();
    }

    @Override
    public void windowDeactivated(WindowEvent arg0) { }

    @Override
    public void windowDeiconified(WindowEvent arg0) { }

    @Override
    public void windowIconified(WindowEvent arg0) { }

    @Override
    public void windowOpened(WindowEvent arg0) { }

    @Override
    public void update(VideoObserversEvents event) {
        switch (event)
        {        
            case resizing:
            {
                pack();
                break;
            }
            case speeding:
            {
                speed.setValue(videoengine.getSpeed());
                break;
            }
            case jumping:
            {            
                break;
            }
        }        
    }

    //keep internal controls up to date during playback
    @Override
    public void update_plays() {
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                timeline.setValue(videoengine.getPosition());
                setTitle(Long.toString(videoengine.getVideoTime()));
            }
        });
        isManualJump = false;
    }
    
    public boolean isCorrectlyInitiliazed() {
        return videoengine != null && videoengine.mediaPlayerFactory != null;
    }
}
