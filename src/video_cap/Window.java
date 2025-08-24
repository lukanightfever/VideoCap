package video_cap;

import java.awt.BorderLayout;  
import java.awt.event.ActionEvent;  
import java.awt.event.ActionListener;  
import java.awt.event.MouseAdapter;  
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.HashMap;
 
import javax.swing.JButton;  
import javax.swing.JFrame;  
import javax.swing.JMenu;  
import javax.swing.JMenuBar;  
import javax.swing.JMenuItem;  
import javax.swing.JPanel;  
import javax.swing.JProgressBar;  
import javax.swing.JSlider;  
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;  
import javax.swing.event.ChangeEvent;  
import javax.swing.event.ChangeListener;  
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.ImageIcon;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;  
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;  

import video_cap.SliderProgressBar;

public class Window extends JFrame{
	private JPanel contentPane;
    private JMenuBar menuBar;
    private JMenu mnFile,mnSetting,mnHelp;
    private JMenuItem mnOpenVideo,mnExit;
    private JPanel panel;
    private SliderProgressBar progress;
    private JPanel progressPanel;
    private JPanel controlPanel;
    private JButton btnStop,btnPlay,btnPause;
    private JSlider slider;
    private JButton btnSwitchPrev, btnSwitchNext;
    private JLabel timeLabel;
    
    // 标注相关组件
    private JPanel annotationPanel;
    private JTextArea annotationTextArea;
    private JButton exportButton;
    private JScrollPane annotationScrollPane;
    public Map<Long, String> annotations = new HashMap<>();
    
    EmbeddedMediaPlayerComponent playerComponent;
      
    public static void main(String[] args) {  
          
    }  
    
    public Window(){  
        setTitle("VideoCaptioner");  
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
        setBounds(200,80,900,700);
        
        try {
          
            ImageIcon icon = new ImageIcon("app.png");
            setIconImage(icon.getImage());
        } catch (Exception e) {
            System.out.println("无法加载图标: " + e.getMessage());
        }
        
        contentPane=new JPanel();  
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));  
        contentPane.setLayout(new BorderLayout(0,0));  
        setContentPane(contentPane);
          
        menuBar=new JMenuBar();  
        setJMenuBar(menuBar);  
          
        mnFile=new JMenu("File");  
        menuBar.add(mnFile);  
        mnSetting=new JMenu("Settings");  
        menuBar.add(mnSetting);  
        mnHelp=new JMenu("Help");  
        menuBar.add(mnHelp);  
          
        mnOpenVideo =new JMenuItem("Open file...");
        mnFile.add(mnOpenVideo);  
          
        mnExit =new JMenuItem("Exit");
        mnFile.add(mnExit);  
          
        mnOpenVideo.addActionListener(new ActionListener() {  
            @Override  
            public void actionPerformed(ActionEvent e) {  
                Player.openVideo();  
            }  
        });  
          
        mnExit.addActionListener(new ActionListener() {  
            @Override  
            public void actionPerformed(ActionEvent e) {  
                Player.exit();  
            }  
        });  
          
        JPanel videoPane=new JPanel();  
        contentPane.add(videoPane, BorderLayout.CENTER);  
        videoPane.setLayout(new BorderLayout(0,0));
          
        playerComponent=new EmbeddedMediaPlayerComponent();  
        videoPane.add(playerComponent);  
          
        // 创建主控制面板
        JPanel mainControlPanel = new JPanel();
        mainControlPanel.setLayout(new BorderLayout());
        videoPane.add(mainControlPanel, BorderLayout.SOUTH);
          
        // 控制按钮面板
        controlPanel=new JPanel();
        mainControlPanel.add(controlPanel, BorderLayout.NORTH);
          
        btnStop=new JButton("Restart");  
        btnStop.addMouseListener(new MouseAdapter() {  
            @Override  
            public void mouseClicked(MouseEvent e) {  
                Player.stop();  
            }  
        });  
        controlPanel.add(btnStop);  
        
        btnSwitchPrev = new JButton("Previous");
        btnSwitchPrev.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Player.switchToPreviousVideo();
            }
        });
        controlPanel.add(btnSwitchPrev);
        
        btnPlay=new JButton("Play");  
        btnPlay.addMouseListener(new MouseAdapter() {  
            @Override  
            public void mouseClicked(MouseEvent e) {  
                Player.play();  
            }  
        });  
        controlPanel.add(btnPlay);  
          
        btnPause=new JButton("Stop");  
        btnPause.addMouseListener(new MouseAdapter() {  
            @Override  
            public void mouseClicked(MouseEvent e) {  
                Player.pause();  
            }  
        });  
        controlPanel.add(btnPause);  
        
        btnSwitchNext = new JButton("Next");
        btnSwitchNext.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Player.switchToNextVideo();
            }
        });
        controlPanel.add(btnSwitchNext);
        
        slider=new JSlider();  
        slider.setValue(80);  
        slider.setMaximum(100);  
        slider.addChangeListener(new ChangeListener() {  
            @Override  
            public void stateChanged(ChangeEvent e) {  
                Player.setVol(slider.getValue());  
            }  
        });  
        controlPanel.add(slider);  
        
        // 进度条面板 - 放在按钮下方
        progressPanel=new JPanel();
        progressPanel.setLayout(new BorderLayout());
        mainControlPanel.add(progressPanel, BorderLayout.SOUTH);
        
        // 时间标签
        timeLabel = new JLabel("00:00");
        timeLabel.setPreferredSize(new java.awt.Dimension(80, 20));
        progressPanel.add(timeLabel, BorderLayout.WEST);
        
        // 进度条 - 设置更长的尺寸
        progress=new SliderProgressBar();
        progress.setPreferredSize(new java.awt.Dimension(700, 20)); // 设置更长的进度条
        progressPanel.add(progress, BorderLayout.CENTER);  
        
        progress.addProgressDragListener(new SliderProgressBar.ProgressDragListener() {
            @Override
            public void onProgressDrag(float percent) {
                Player.jumpTo(percent);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                Player.updateAnnotationDisplay();
            }
            
            @Override public void mouseClicked(MouseEvent e) {}
            @Override public void mousePressed(MouseEvent e) {}
            @Override public void mouseEntered(MouseEvent e) {}
            @Override public void mouseExited(MouseEvent e) {}
        });
        
        progress.setStringPainted(true);
        
        /*标注区域 - 从左到右书写*/
        annotationPanel = new JPanel(new BorderLayout());
        annotationPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        annotationPanel.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
        contentPane.add(annotationPanel, BorderLayout.SOUTH);

        // 标注标签
        JLabel annotationLabel = new JLabel("Annotations:");
        annotationLabel.setHorizontalAlignment(JLabel.LEFT);
        annotationPanel.add(annotationLabel, BorderLayout.NORTH);

        // 标注文本框 - 确保从左到右书写
        annotationTextArea = new JTextArea();
        annotationTextArea.setRows(3);
        annotationTextArea.setLineWrap(true);
        annotationTextArea.setWrapStyleWord(true);
        annotationTextArea.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
        annotationTextArea.setFont(new java.awt.Font("Microsoft YaHei", java.awt.Font.PLAIN, 12));
        annotationTextArea.setFocusable(true);
        annotationTextArea.setEditable(true);
        
        
        // 设置初始光标位置
        annotationTextArea.setCaretPosition(0);

        annotationScrollPane = new JScrollPane(annotationTextArea);
        annotationScrollPane.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
        annotationPanel.add(annotationScrollPane, BorderLayout.CENTER);

        // 导出按钮
        JPanel buttonPanel = new JPanel();
        buttonPanel.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
        exportButton = new JButton("Save");
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Player.exportAnnotations();
            }
        });
        buttonPanel.add(exportButton);
        annotationPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 文档监听器实现实时保存
        annotationTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                Player.saveCurrentAnnotation();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                Player.saveCurrentAnnotation();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                Player.saveCurrentAnnotation();
            }
        });

        // 焦点监听器确保始终从左开始
        annotationTextArea.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                annotationTextArea.setCaretPosition(0);
            }
        });
    }   
      
    public EmbeddedMediaPlayer getMediaPlayer() {  
        return playerComponent.getMediaPlayer();  
    }  
      
    public SliderProgressBar getProgressBar() {  
        return progress;  
    }  
    
    public void setTimeText(String timeText) {
        if (timeLabel != null) {
            timeLabel.setText(timeText);
        }
    }
    
    public Map<Long, String> getAnnotations() {
        return annotations;
    }
    
    public JTextArea getAnnotationTextArea() {
        return annotationTextArea;
    }
    
    public void clearAnnotations() {
        annotations.clear();
        if (annotationTextArea != null) {
            int caretPosition = annotationTextArea.getCaretPosition();
            annotationTextArea.setText("");
            annotationTextArea.setCaretPosition(0);
        }
    }
}