package video_cap;

import java.awt.EventQueue;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

public class Player {
    
    //static {
    //    System.loadLibrary("libvlc"); 
    //}
    
    static Window frame;
    private static int times = 1;
    private static boolean flag = true;
    private static File currentVideoFile = null;
    
    public static void main(String[] args) {
         
        NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "vlcj//lib");
        Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);

        EventQueue.invokeLater(new Runnable() {
 
            @Override
            public void run() {
                    try {
                        frame = new Window();
                        frame.setVisible(true);
                        frame.getMediaPlayer().playMedia("video//style.avi");
                        
                        new SwingWorker<String, Integer>() {
 
                            @Override
                            protected String doInBackground() throws Exception {
                                while (flag) {
                                    long total = frame.getMediaPlayer().getLength();
                                    if(total == -1){
                                        play();
                                        times++;
                                        //System.out.println(times + "******************");
                                    }
                                    long curr = frame.getMediaPlayer().getTime();
                                    float percent = (float) curr / total;
                                    publish((int) (percent * 100));
                                    
                                    updateTimeDisplay(curr, total);
                                    
                                    Thread.sleep(100);
                                }
                                return null;
                            }
 
                            protected void process(java.util.List<Integer> chunks) {
                                for (int v : chunks) {
                                    frame.getProgressBar().setValue(v);
                                }
                            }
                        }.execute();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
 
        });
    }
    
    public static void dispose(){
        flag = false;
        frame.getMediaPlayer().stop();
        frame.dispose();
    }
 
    public static void openVideo() {
        JFileChooser chooser = new JFileChooser();
        int v = chooser.showOpenDialog(null);
        if (v == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            currentVideoFile = file;
            frame.getMediaPlayer().playMedia(file.getAbsolutePath());
            
            frame.setTimeText("00:00 / 00:00");
            frame.clearAnnotations();
            
            // 尝试加载同名的JSON标注文件
            loadAnnotationsFromJson();
        }
    }
    
    private static void loadAnnotationsFromJson() {
        if (currentVideoFile == null) {
            return;
        }
        
        try {
            String videoName = currentVideoFile.getName();
            String baseName = videoName.substring(0, videoName.lastIndexOf('.'));
            File jsonFile = new File(currentVideoFile.getParentFile(), baseName + ".json");
            
            if (jsonFile.exists()) {
                // 读取JSON文件内容
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(jsonFile));
                StringBuilder jsonContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonContent.append(line);
                }
                reader.close();
                
                // 解析JSON
                String content = jsonContent.toString().trim();
                if (content.startsWith("[") && content.endsWith("]")) {
                    content = content.substring(1, content.length() - 1).trim();
                    
                    if (!content.isEmpty()) {
                        // 清除现有标注
                        frame.getAnnotations().clear();
                        
                        // 分割各个标注对象
                        String[] entries = content.split("\\},\\s*\\{");
                        for (String entry : entries) {
                            entry = entry.trim();
                            if (entry.startsWith("{")) {
                                entry = entry.substring(1);
                            }
                            if (entry.endsWith("}")) {
                                entry = entry.substring(0, entry.length() - 1);
                            }
                            
                            // 解析时间和标注内容
                            String[] parts = entry.split(",\\s*\"annotation\"\\s*:");
                            if (parts.length == 2) {
                                String timePart = parts[0].trim();
                                String annotationPart = parts[1].trim();
                                
                                if (timePart.startsWith("\"time\":")) {
                                    long time = Long.parseLong(timePart.substring(7).trim());
                                    String annotation = annotationPart.substring(1, annotationPart.length() - 1)
                                            .replace("\\\"", "\"")  // 只处理双引号转义
                                            .replace("\\n", "\n")   // 处理换行
                                            .replace("\\r", "\r");   // 处理回车
                                    
                                    frame.getAnnotations().put(time, annotation);
                                }
                            }
                        }
                        
                        // 更新显示
                        Player.updateAnnotationDisplay();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("加载标注文件失败: " + e.getMessage());
            // 不显示错误提示
        }
    }
    
    public static void exit() {
        frame.getMediaPlayer().release();
        System.exit(0);
    }
 
    public static void play() {
        frame.getMediaPlayer().play();
    }
 
    public static void pause() {
        frame.getMediaPlayer().pause();
    }
 
    public static void stop() {
        frame.getMediaPlayer().stop();
    }
 
    public static void jumpTo(float to) {
        long newTime = (long) (to * frame.getMediaPlayer().getLength());
        frame.getMediaPlayer().setTime(newTime);
        
        // 更新标注显示，但保持光标位置
        JTextArea textArea = frame.getAnnotationTextArea();
        int caretPosition = textArea.getCaretPosition();
        
        long currentTimeSeconds = newTime / 1000;
        String annotation = frame.getAnnotations().get(currentTimeSeconds);
        textArea.setText(annotation != null ? annotation : "");
        
        // 恢复光标位置
        if (caretPosition <= textArea.getText().length()) {
            textArea.setCaretPosition(caretPosition);
        } else {
            textArea.setCaretPosition(textArea.getText().length());
        }
    }
 
    public static void setVol(int v) {
        frame.getMediaPlayer().setVolume(v);
    }
    
    public static void switchToPreviousVideo() {
        if (currentVideoFile != null && currentVideoFile.exists()) {
            // 保存当前标注
            saveCurrentAnnotation();
            
            File directory = currentVideoFile.getParentFile();
            File[] videoFiles = getVideoFiles(directory);
            
            if (videoFiles != null && videoFiles.length > 0) {
                int currentIndex = findFileIndex(videoFiles, currentVideoFile);
                if (currentIndex > 0) {
                    File previousFile = videoFiles[currentIndex - 1];
                    currentVideoFile = previousFile;
                    frame.getMediaPlayer().playMedia(previousFile.getAbsolutePath());
                    frame.clearAnnotations();
                    
                    // 加载新视频的标注
                    loadAnnotationsFromJson();
                }
            }
        }
    }

    public static void switchToNextVideo() {
        if (currentVideoFile != null && currentVideoFile.exists()) {
            // 保存当前标注
            saveCurrentAnnotation();
            
            File directory = currentVideoFile.getParentFile();
            File[] videoFiles = getVideoFiles(directory);
            
            if (videoFiles != null && videoFiles.length > 0) {
                int currentIndex = findFileIndex(videoFiles, currentVideoFile);
                if (currentIndex < videoFiles.length - 1) {
                    File nextFile = videoFiles[currentIndex + 1];
                    currentVideoFile = nextFile;
                    frame.getMediaPlayer().playMedia(nextFile.getAbsolutePath());
                    frame.clearAnnotations();
                    
                    // 加载新视频的标注
                    loadAnnotationsFromJson();
                }
            }
        }
    }
    
    private static File[] getVideoFiles(File directory) {
        File[] files = directory.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".mp4") || lowerName.endsWith(".avi") || 
                   lowerName.endsWith(".mkv") || lowerName.endsWith(".mov") ||
                   lowerName.endsWith(".wmv") || lowerName.endsWith(".flv");
        });
        
        if (files != null) {
            Arrays.sort(files, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        }
        
        return files;
    }
    
    private static int findFileIndex(File[] files, File targetFile) {
        for (int i = 0; i < files.length; i++) {
            if (files[i].getAbsolutePath().equals(targetFile.getAbsolutePath())) {
                return i;
            }
        }
        return -1;
    }
    
    public static String formatTime(long milliseconds) {
        if (milliseconds < 0) {
            return "00:00";
        }
        
        long totalSeconds = milliseconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    public static void updateTimeDisplay(long currentTime, long totalTime) {
        if (frame != null) {
            String currentTimeStr = formatTime(currentTime);
            String totalTimeStr = formatTime(totalTime);
            
            frame.setTimeText(currentTimeStr + " / " + totalTimeStr);
            
            // 自动更新标注显示，但保持光标位置
            JTextArea textArea = frame.getAnnotationTextArea();
            int caretPosition = textArea.getCaretPosition();
            
            long currentTimeSeconds = currentTime / 1000;
            String annotation = frame.getAnnotations().get(currentTimeSeconds);
            textArea.setText(annotation != null ? annotation : "");
            
            // 恢复光标位置
            if (caretPosition <= textArea.getText().length()) {
                textArea.setCaretPosition(caretPosition);
            } else {
                textArea.setCaretPosition(textArea.getText().length());
            }
        }
    }
    
    public static void updateAnnotationDisplay() {
        if (frame != null && frame.getMediaPlayer() != null) {
            JTextArea textArea = frame.getAnnotationTextArea();
            
            // 保存当前光标位置和选择范围
            int caretPosition = textArea.getCaretPosition();
            int selectionStart = textArea.getSelectionStart();
            int selectionEnd = textArea.getSelectionEnd();
            
            long currentTimeSeconds = frame.getMediaPlayer().getTime() / 1000;
            String annotation = frame.getAnnotations().get(currentTimeSeconds);
            textArea.setText(annotation != null ? annotation : "");
            
            // 恢复光标位置和选择范围
            if (caretPosition <= textArea.getText().length()) {
                textArea.setCaretPosition(caretPosition);
            } else {
                textArea.setCaretPosition(textArea.getText().length());
            }
            
            // 恢复选择范围（如果有）
            if (selectionStart != selectionEnd && 
                selectionStart <= textArea.getText().length() && 
                selectionEnd <= textArea.getText().length()) {
                textArea.select(selectionStart, selectionEnd);
            }
        }
    }
    
    public static void saveCurrentAnnotation() {
        if (frame != null && frame.getMediaPlayer() != null) {
            long currentTimeSeconds = frame.getMediaPlayer().getTime() / 1000;
            JTextArea textArea = frame.getAnnotationTextArea();
            
            // 保存当前状态
            int caretPosition = textArea.getCaretPosition();
            int selectionStart = textArea.getSelectionStart();
            int selectionEnd = textArea.getSelectionEnd();
            String text = textArea.getText();
            
            String annotation = text; // 直接使用原始文本，不trim
            
            if (!annotation.isEmpty()) {
                frame.getAnnotations().put(currentTimeSeconds, annotation);
                // 调试信息
            } 
            else {
                if (frame.getAnnotations().containsKey(currentTimeSeconds)) {
                    frame.getAnnotations().remove(currentTimeSeconds);
                  
                }
            }
            
            // 恢复文本和光标状态
            if (!textArea.getText().equals(text)) {
                textArea.setText(text);
            }
            
            // 恢复光标位置
            if (caretPosition <= textArea.getText().length()) {
                textArea.setCaretPosition(caretPosition);
            }
            
            // 恢复选择范围
            if (selectionStart != selectionEnd && 
                selectionStart <= textArea.getText().length() && 
                selectionEnd <= textArea.getText().length()) {
                textArea.select(selectionStart, selectionEnd);
            }
        }
    }
    
    public static void exportAnnotations() {
    if (frame == null || frame.getAnnotations().isEmpty()) {
        return;
    }
    
    if (currentVideoFile == null) {
        return;
    }
    
    try {
        // 创建与视频同名的JSON文件
        String videoName = currentVideoFile.getName();
        String baseName = videoName.substring(0, videoName.lastIndexOf('.'));
        File jsonFile = new File(currentVideoFile.getParentFile(), baseName + ".json");
        
        // 构建JSON格式的标注数据
        StringBuilder jsonOutput = new StringBuilder();
        jsonOutput.append("[\n");
        
        boolean firstEntry = true;
        for (Map.Entry<Long, String> entry : frame.getAnnotations().entrySet()) {
            if (!firstEntry) {
                jsonOutput.append(",\n");
            }
            
            // 只转义必要的字符
            String escapedAnnotation = entry.getValue()
                .replace("\\", "\\\\")  // 转义反斜杠
                .replace("\"", "\\\"")  // 转义双引号
                .replace("\n", "\\n")   // 转义换行
                .replace("\r", "\\r");  // 转义回车
            
            jsonOutput.append("  {\n")
                      .append("    \"time\": ").append(entry.getKey()).append(",\n")
                      .append("    \"annotation\": \"").append(escapedAnnotation).append("\"\n")
                      .append("  }");
            firstEntry = false;
        }
        
        jsonOutput.append("\n]");
        
        // 写入文件
        java.io.FileWriter writer = new java.io.FileWriter(jsonFile);
        writer.write(jsonOutput.toString());
        writer.close();
        
    } catch (Exception e) {
        System.out.println("导出失败: " + e.getMessage());
    }
    }
}