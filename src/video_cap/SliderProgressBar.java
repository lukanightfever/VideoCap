package video_cap;

import javax.swing.JProgressBar;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class SliderProgressBar extends JProgressBar {
    private boolean isDragging = false;
    
    public SliderProgressBar() {
        // 添加鼠标监听器
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isOverSlider(e.getX())) {
                    isDragging = true;
                    fireDragEvent(e.getX());
                } else {
                    // 点击非滑块区域直接跳转
                    fireDragEvent(e.getX());
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    fireDragEvent(e.getX());
                }
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 在进度条上绘制一个简单的滑块标识
        int sliderPos = (int) (getWidth() * getValue() / 100.0);
        
        // 绘制滑块（小矩形）
        g.setColor(Color.RED);
        g.fillRect(sliderPos - 6, getHeight()/2 - 10, 12, 20);
        
        // 边框
        g.setColor(Color.DARK_GRAY);
        g.drawRect(sliderPos - 6, getHeight()/2 - 10, 12, 20);
    }
    
    private boolean isOverSlider(int mouseX) {
        int sliderPos = (int) (getWidth() * getValue() / 100.0);
        return mouseX >= sliderPos - 6 && mouseX <= sliderPos + 6;
    }
    
    private void fireDragEvent(int mouseX) {
        float percent = (float) mouseX / getWidth();
        percent = Math.max(0, Math.min(1, percent));
        
        // 通知监听器
        for (java.awt.event.MouseListener listener : getMouseListeners()) {
            if (listener instanceof ProgressDragListener) {
                ((ProgressDragListener) listener).onProgressDrag(percent);
            }
        }
    }
    
    public interface ProgressDragListener extends java.awt.event.MouseListener {
        void onProgressDrag(float percent);
    }
    
    public void addProgressDragListener(ProgressDragListener listener) {
        addMouseListener(listener);
    }
}