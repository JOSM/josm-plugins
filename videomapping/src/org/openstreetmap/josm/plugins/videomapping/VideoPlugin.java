package org.openstreetmap.josm.plugins.videomapping;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.InputVerifier;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.text.MaskFormatter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.videomapping.video.GPSVideoPlayer;
import org.openstreetmap.josm.plugins.videomapping.video.VideoEngine;
import org.openstreetmap.josm.tools.Shortcut;

import uk.co.caprica.vlcj.player.DeinterlaceMode;

  /**
 * @author Matthias Meißer (digi_c at arcor dot de)
 * @ released under GPL
 * This Plugin allows you to link multiple videos against a GPS track and playback both synchronously 
 */

//Here we manage properties and start the other classes
public class VideoPlugin extends Plugin implements LayerChangeListener{
	private JMenu VMenu,VDeinterlacer;
	private JosmAction VAdd,VRemove,VStart,Vbackward,Vforward,VJump,Vfaster,Vslower,Vloop;
	private JRadioButtonMenuItem VIntBob,VIntNone,VIntLinear;
    private JCheckBoxMenuItem VCenterIcon,VSubTitles;
    private JMenuItem VJumpLength,VLoopLength;
    private final String PROP_MRU="videomapping.mru";
    private final String PROP_AUTOCENTER="videomapping.autocenter";
    private final String PROP_JUMPLENGTH="videomapping.jumplength";
    private final String PROP_LOOPLENGTH="videomapping.looplength"; 
//    private String deinterlacer;
    private boolean autoCenter;
    private Integer jumpLength,loopLength;
    private String mostRecentFolder;
	private GpxLayer gpsLayer;
	private VideoPositionLayer videoPositionLayer;
	private GPSVideoPlayer gpsVideoPlayer;

	public VideoPlugin(PluginInformation info) {
		super(info);
		VideoEngine.setupPlayer();
		MapView.addLayerChangeListener(this);				
		createMenusAndShortCuts();
		enableVideoControlMenus(false);
		setDefaults();
		loadProperties();
	}

	private void createMenusAndShortCuts() {
		VMenu = Main.main.menu.addMenu(marktr("Video"), KeyEvent.VK_D, Main.main.menu.defaultMenuPos,ht("/Plugin/Videomapping"));
		VMenu.setEnabled(false);
		VAdd= new JosmAction(tr("Import Video"),"videomapping",tr("Sync a video against this GPS track"),null,false) {
            public void actionPerformed(ActionEvent arg0) {                 
                    importVideoFile();
                }
        };
        VRemove= new JosmAction(tr("Remove Video"),"videomapping",tr("removes current video from layer"),null,false) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent arg0) {
            }
        };
        VStart = new JosmAction(tr("Play/Pause"), "audio-playpause", tr("starts/pauses video playback"),
                Shortcut.registerShortcut("videomapping:startstop",tr("Video: {0}", tr("Play/Pause")),KeyEvent.VK_NUMPAD5, Shortcut.DIRECT), false, "vm_play_pause",false) {            
            public void actionPerformed(ActionEvent e) {
                if (gpsVideoPlayer != null) {
                    gpsVideoPlayer.pause();
                }
            }
        };
        Vbackward = new JosmAction(tr("Backward"), "audio-prev", tr("jumps n sec back"),
                Shortcut.registerShortcut("videomapping:backward",tr("Video: {0}", tr("Backward")),KeyEvent.VK_NUMPAD4, Shortcut.DIRECT), false, "vm_prev",false) {
            public void actionPerformed(ActionEvent e) {
                if (gpsVideoPlayer != null) {
                    gpsVideoPlayer.backward();
                }
            }
        };
        Vforward= new JosmAction(tr("Forward"), "audio-next", tr("jumps n sec forward"),
                Shortcut.registerShortcut("videomapping:forward",tr("Video: {0}", tr("Forward")),KeyEvent.VK_NUMPAD6, Shortcut.DIRECT), false, "vm_next",false) {
            public void actionPerformed(ActionEvent e) {
                if (gpsVideoPlayer != null) {
                    gpsVideoPlayer.forward();
                }
            }
        };
        Vfaster= new JosmAction(tr("Faster"), "audio-faster", tr("faster playback"),
                Shortcut.registerShortcut("videomapping:faster",tr("Video: {0}", tr("Faster")),KeyEvent.VK_NUMPAD8, Shortcut.DIRECT), false, "vm_faster",false) {
            
            public void actionPerformed(ActionEvent e) {
                if (gpsVideoPlayer != null) {
                    gpsVideoPlayer.setSpeed(gpsVideoPlayer.getSpeed()+20);
                }
            }
        };
        Vslower= new JosmAction(tr("Slower"), "audio-slower", tr("slower playback"),
                Shortcut.registerShortcut("videomapping:slower",tr("Video: {0}", tr("Slower")),KeyEvent.VK_NUMPAD2, Shortcut.DIRECT), false, "vm_slower",false) {
            
            public void actionPerformed(ActionEvent e) {
                if (gpsVideoPlayer != null) {
                    gpsVideoPlayer.setSpeed(gpsVideoPlayer.getSpeed()-20);
                }
            }
        };
        VJump= new JosmAction(tr("Jump To"), "jumpto", tr("jumps to the entered gps time"),null, false) {
            public void actionPerformed(ActionEvent e) {
            	showJumpTo();
            }
                            
            
        };
        Vloop= new JosmAction(tr("Loop"), "loop", tr("loops n sec around current position"),
                Shortcut.registerShortcut("videomapping:loop",tr("Video: {0}", tr("Loop")),KeyEvent.VK_NUMPAD7, Shortcut.DIRECT), false) {            
            public void actionPerformed(ActionEvent e) {
                if (gpsVideoPlayer != null) {
                    gpsVideoPlayer.toggleLooping();
                }
            }
        };
        
        //now the options menu
        VCenterIcon = new JCheckBoxMenuItem(new JosmAction(tr("Keep centered"), (String)null, tr("follows the video icon automaticly"),null, false,"vm_keepcentered",false) {            
            public void actionPerformed(ActionEvent e) {
                if (videoPositionLayer != null) {
                    videoPositionLayer.setAutoCenter(VCenterIcon.isSelected());
                }
            }
        });
        
        VSubTitles = new JCheckBoxMenuItem(new JosmAction(tr("Subtitles"), (String)null, tr("Show subtitles in video"),null, false,"vm_subtitles",false) {
            public void actionPerformed(ActionEvent e) {
                if (gpsVideoPlayer != null) {
                    gpsVideoPlayer.setSubtitles(VSubTitles.isSelected());
                }
            }
        });
        
        VJumpLength = new JMenuItem(new JosmAction(tr("Jump length"), (String)null, tr("Set the length of a jump"),null, false,"vm_jumplen",false) {            
            public void actionPerformed(ActionEvent e) {
            	Object[] possibilities = {"200", "500", "1000", "2000", "10000"};
                String s = (String)JOptionPane.showInputDialog(Main.parent,tr("Jump in video for x ms"),tr("Jump length"),JOptionPane.QUESTION_MESSAGE,null,possibilities,jumpLength);
                jumpLength=Integer.getInteger(s);
                saveProperties();
            }
        });
        
        VLoopLength = new JMenuItem(new JosmAction(tr("Loop length"), (String)null, tr("Set the length around a looppoint"),null, false,"vm_looplen",false) {            
            public void actionPerformed(ActionEvent e) {
            	Object[] possibilities = {"500", "1000", "3000", "5000", "10000"};
                String s = (String)JOptionPane.showInputDialog(Main.parent,tr("Jump in video for x ms"),tr("Loop length"),JOptionPane.QUESTION_MESSAGE,null,possibilities,loopLength);
                loopLength=Integer.getInteger(s);
                saveProperties();
            }
        });
        //TODO read deinterlacers list out of videoengine
        VDeinterlacer= new JMenu("Deinterlacer");
        VIntNone= new JRadioButtonMenuItem(new JosmAction(tr("none"), (String)null, tr("no deinterlacing"),null, false,"vm_deinterlacer",false) {            
            public void actionPerformed(ActionEvent e) {
                if (gpsVideoPlayer != null) {
                    gpsVideoPlayer.setDeinterlacer(null);
                }
            }
        });
        VIntBob= new JRadioButtonMenuItem(new JosmAction("bob", (String)null, tr("deinterlacing using line doubling"),null, false,"vm_bobdeinterlace",false) {
            public void actionPerformed(ActionEvent e) {
                if (gpsVideoPlayer != null) {
                    gpsVideoPlayer.setDeinterlacer(DeinterlaceMode.BOB);
                }
            }
        });
        VIntLinear= new JRadioButtonMenuItem(new JosmAction("linear", (String)null, tr("deinterlacing using linear interpolation"),null, false,"vm_lineardeinterlace",false) {
            public void actionPerformed(ActionEvent e) {
                if (gpsVideoPlayer != null) {
                    gpsVideoPlayer.setDeinterlacer(DeinterlaceMode.LINEAR);
                }
            }
        });
        VDeinterlacer.add(VIntNone);
        VDeinterlacer.add(VIntBob);
        VDeinterlacer.add(VIntLinear);        
        VMenu.add(VAdd);        
        VMenu.add(VStart);
        VMenu.add(Vbackward);
        VMenu.add(Vforward);
        VMenu.add(Vfaster);
        VMenu.add(Vslower);
        VMenu.add(Vloop);
        VMenu.add(VJump);
        VMenu.addSeparator();
        VMenu.add(VCenterIcon);
        VMenu.add(VJumpLength);
        VMenu.add(VLoopLength);
        VMenu.add(VDeinterlacer);
        VMenu.add(VSubTitles);
    }

	protected void importVideoFile() {
		JFileChooser fc = new JFileChooser(mostRecentFolder);
        fc.setSelectedFile(new File(mostRecentFolder));
        if (fc.showOpenDialog(Main.parent) != JFileChooser.CANCEL_OPTION) {
        	mostRecentFolder=fc.getSelectedFile().getAbsolutePath();
        	saveProperties();
        	if (videoPositionLayer == null && gpsLayer != null) {
        		videoPositionLayer = new VideoPositionLayer(gpsLayer);
            	gpsVideoPlayer = new GPSVideoPlayer(new SimpleDateFormat("hh:mm:ss") ,videoPositionLayer);
            	gpsVideoPlayer.setJumpLength(jumpLength);
            	gpsVideoPlayer.setLoopLength(loopLength);
            	enableVideoControlMenus(true);
        	}
        	if (gpsVideoPlayer != null) {
        	    gpsVideoPlayer.addVideo(fc.getSelectedFile());
        	}
        }		
	}

	private void enableVideoControlMenus(boolean b) {
		VStart.setEnabled(b);
        Vbackward.setEnabled(b);
        Vforward.setEnabled(b);
        Vloop.setEnabled(b);
        Vfaster.setEnabled(b);
        Vslower.setEnabled(b);
        VJump.setEnabled(b);		
	}
	
	private void setDefaults() {
		autoCenter=false;
//		deinterlacer="";
		jumpLength=1000;
		loopLength=6000;
		mostRecentFolder=System.getProperty("user.home");		
	}
	
	private void loadProperties() {
        String temp;        
        temp=Main.pref.get(PROP_AUTOCENTER);
        if((temp!=null)&&(temp.length()!=0))
        	autoCenter=Boolean.getBoolean(temp);        
        temp=Main.pref.get(PROP_JUMPLENGTH);
        if((temp!=null)&&(temp.length()!=0))
        	jumpLength=Integer.valueOf(temp);
        temp=Main.pref.get(PROP_LOOPLENGTH);
        if((temp!=null)&&(temp.length()!=0))
        	loopLength=Integer.valueOf(temp);
        temp=Main.pref.get(PROP_MRU);
        if((temp!=null)&&(temp.length()!=0))
        	mostRecentFolder=Main.pref.get(PROP_MRU);        
    }
	
	private void saveProperties(){
        Main.pref.put(PROP_AUTOCENTER, autoCenter);
        Main.pref.put(PROP_JUMPLENGTH, jumpLength.toString());
        Main.pref.put(PROP_LOOPLENGTH, loopLength.toString());  
    	Main.pref.put(PROP_MRU, mostRecentFolder);
    }
	
	private void showJumpTo()
	{
    	try {
	    	JOptionPane d=new JOptionPane(tr("Jump to"), JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);	    	
	    	SimpleDateFormat gpsTimeFormat= new SimpleDateFormat("HH:mm:ss");
	    	String timerange=gpsTimeFormat.format(videoPositionLayer.getFirstWayPoint().getTime())+" - ";
	    	timerange=timerange+gpsTimeFormat.format(videoPositionLayer.getLastWayPoint().getTime());
	    	d.add(new JLabel(timerange)); //TODO for some reason this doesn't work -> use dialog
	    	final JFormattedTextField inp = new JFormattedTextField(new MaskFormatter("##:##:##"));
	    	inp.setText(gpsTimeFormat.format( videoPositionLayer.getGPSDate()));
	    	inp.setInputVerifier(new InputVerifier() {					
				@Override
				public boolean verify(JComponent input) {
					return false;
				}
			});
	    	//hack to set the focus
	    	SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	            	inp.requestFocus();
	            	inp.setCaretPosition(0);
	            }
	        });
	    	if (JOptionPane.showConfirmDialog(Main.panel,inp, tr("Jump to GPS time"),JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
	    	{
	    		//add the day to the time
	    		Date t = gpsTimeFormat.parse(inp.getText());	    		
	    		Calendar time = Calendar.getInstance();
	    		Calendar date = Calendar.getInstance();
	    		time.setTime(t);
	    		date.setTime(videoPositionLayer.getFirstWayPoint().getTime());
	    		time.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DATE));
	            if (t!=null)
	            {
	            	videoPositionLayer.jump(time.getTime());
	            }                       
	    	}
    	} catch (ParseException e1) {
                e1.printStackTrace();
        }
	}
	

	public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        VMenu.setEnabled(true);
        if (newLayer instanceof GpxLayer)
        {
            VAdd.setEnabled(true);
            gpsLayer=((GpxLayer) newLayer);            
            //TODO append to GPS Layer menu
        }        
    }

	public void layerAdded(Layer arg0) {
		activeLayerChange(null,arg0);
		
	}

	public void layerRemoved(Layer arg0) {
		if(arg0 instanceof VideoPositionLayer)
			enableVideoControlMenus(false);
		activeLayerChange(null,arg0);
		
	}
  }
