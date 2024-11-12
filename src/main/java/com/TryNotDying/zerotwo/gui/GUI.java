package com.TryNotDying.zerotwo.gui;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import com.TryNotDying.zerotwo.Bot;

public class GUI extends JFrame 
{
    private final ConsolePanel console;
    private Bot bot;
    
    public GUI() 
    {
        super();
        console = new ConsolePanel();
    }
    
    public void setBot(Bot bot) {
        this.bot = bot;
    }
    
    public void init()
    {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("ZeroTwo");
        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Console", console);
        getContentPane().add(tabs);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        addWindowListener(new WindowListener() 
        {
            @Override public void windowOpened(WindowEvent e) { /* unused */ }
            @Override public void windowClosing(WindowEvent e) 
            {
                try
                {
                    if (bot != null)
                        bot.shutdown();
                }
                catch(Exception ex)
                {
                    System.exit(0);
                }
            }
            @Override public void windowClosed(WindowEvent e) { /* unused */ }
            @Override public void windowIconified(WindowEvent e) { /* unused */ }
            @Override public void windowDeiconified(WindowEvent e) { /* unused */ }
            @Override public void windowActivated(WindowEvent e) { /* unused */ }
            @Override public void windowDeactivated(WindowEvent e) { /* unused */ }
        });
    }
}