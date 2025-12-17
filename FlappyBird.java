import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import javax.swing.*;
import javax.sound.sampled.*;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {

    enum GameState { MENU, RUNNING, GAMEOVER }
    public GameState state = GameState.MENU;

    int boardWidth = 360, boardHeight = 640;

    // Background
    Image sky, clouds, ground;
    float cloudX = 0, groundX = 0;
    float cloudSpeed = 0.2f;
    float groundSpeed = -4f;

    // Bird
    Image[] birdFrames;
    int birdFrame = 0;
    int birdX = boardWidth / 4, birdY = boardHeight / 2;
    int birdWidth = 34, birdHeight = 24;
    float velocityY = 0, gravity = 0.5f, jumpStrength = -8f;
    float birdAngle = 0;

    // Pipes
    int pipeWidth = 64, pipeHeight = 512, pipeGap = 150;
    float pipeSpeed = -4f;
    ArrayList<Pipe> pipes = new ArrayList<>();
    Random random = new Random();

    // Particles
    ArrayList<Particle> particles = new ArrayList<>();

    // Score
    int score = 0, highScore = 0;

    // Timers (explicitly javax.swing.Timer to avoid ambiguity)
    javax.swing.Timer gameLoop;
    javax.swing.Timer pipeSpawner;
    javax.swing.Timer birdAnimator;
    javax.swing.Timer particleTimer;

    // Sounds
    Clip jumpSound, hitSound, scoreSound;

    class Pipe {
        float x, y;
        boolean passed = false;
        Pipe(float x, float y) { this.x = x; this.y = y; }
    }

    class Particle {
        float x, y, alpha = 1f, size;
        Particle(float x, float y, float size) { this.x = x; this.y = y; this.size = size; }
        void update() { y += 1; alpha -= 0.03f; if(alpha < 0) alpha = 0; }
        boolean isDead() { return alpha <= 0; }
    }

    public FlappyBird() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);

        loadResources();

        // Timers
        gameLoop = new javax.swing.Timer(1000/60, this);
        pipeSpawner = new javax.swing.Timer(1500, e -> spawnPipe());
        birdAnimator = new javax.swing.Timer(150, e -> { birdFrame = (birdFrame + 1) % birdFrames.length; repaint(); });
        particleTimer = new javax.swing.Timer(30, e -> updateParticles());

        // Start timers
        gameLoop.start();
        pipeSpawner.start();
        birdAnimator.start();
        particleTimer.start();
    }

    void loadResources() {
        try {
            // Make sure resources are in src/resources/images/
            sky = new ImageIcon(getClass().getResource("/resources/images/sky.png")).getImage();
            clouds = new ImageIcon(getClass().getResource("/resources/images/clouds.png")).getImage();
            ground = new ImageIcon(getClass().getResource("/resources/images/ground.png")).getImage();

            birdFrames = new Image[3];
            birdFrames[0] = new ImageIcon(getClass().getResource("/resources/images/bird1.png")).getImage();
            birdFrames[1] = new ImageIcon(getClass().getResource("/resources/images/bird2.png")).getImage();
            birdFrames[2] = new ImageIcon(getClass().getResource("/resources/images/bird3.png")).getImage();

            // Sounds
            jumpSound = AudioSystem.getClip();
            jumpSound.open(AudioSystem.getAudioInputStream(getClass().getResource("/resources/sounds/jump.wav")));
            hitSound = AudioSystem.getClip();
            hitSound.open(AudioSystem.getAudioInputStream(getClass().getResource("/resources/sounds/hit.wav")));
            scoreSound = AudioSystem.getClip();
            scoreSound.open(AudioSystem.getAudioInputStream(getClass().getResource("/resources/sounds/score.wav")));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    void spawnPipe() {
        if(state != GameState.RUNNING) return;
        float y = -random.nextInt(200) - 100;
        pipes.add(new Pipe(boardWidth, y));
    }

    void addParticles(float x, float y) {
        for(int i = 0; i < 10; i++) {
            particles.add(new Particle(x + random.nextInt(20) - 10, y + random.nextInt(20) - 10, random.nextInt(4) + 2));
        }
    }

    void updateParticles() {
        for(Iterator<Particle> it = particles.iterator(); it.hasNext();) {
            Particle p = it.next();
            p.update();
            if(p.isDead()) it.remove();
        }
    }

    void playSound(Clip clip) {
        if(clip != null) { clip.setFramePosition(0); clip.start(); }
    }

    void updateGame() {
        velocityY += gravity;
        birdY += velocityY;
        birdAngle = Math.min(velocityY * 3, 45);
        if(velocityY < 0) birdAngle = -25;
        if(birdY < 0) birdY = 0;
        if(birdY + birdHeight > boardHeight - 50) {
            state = GameState.GAMEOVER;
            playSound(hitSound);
            addParticles(birdX, birdY + birdHeight/2);
        }

        cloudX += cloudSpeed;
        if(cloudX > boardWidth) cloudX = 0;
        groundX += groundSpeed;
        if(groundX <= -boardWidth) groundX = 0;

        ArrayList<Pipe> remove = new ArrayList<>();
        Rectangle birdRect = new Rectangle(birdX, birdY, birdWidth, birdHeight);
        for(Pipe p : pipes) {
            p.x += pipeSpeed;
            Rectangle topPipe = new Rectangle((int)p.x, (int)p.y, pipeWidth, pipeHeight);
            Rectangle bottomPipe = new Rectangle((int)p.x, (int)p.y + pipeHeight + pipeGap, pipeWidth, pipeHeight);

            if(!p.passed && p.x + pipeWidth < birdX) {
                score++;
                p.passed = true;
                playSound(scoreSound);
            }

            if(birdRect.intersects(topPipe) || birdRect.intersects(bottomPipe)) {
                state = GameState.GAMEOVER;
                playSound(hitSound);
                addParticles(birdX, birdY + birdHeight/2);
            }

            if(p.x + pipeWidth < 0) remove.add(p);
        }
        pipes.removeAll(remove);
        if(score > highScore) highScore = score;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if(sky != null) g2d.drawImage(sky, 0, 0, boardWidth, boardHeight, null);
        else { g2d.setColor(Color.cyan); g2d.fillRect(0,0,boardWidth,boardHeight); }

        if(clouds != null){
            g2d.drawImage(clouds, (int)-cloudX, 50, boardWidth, 100, null);
            g2d.drawImage(clouds, (int)(-cloudX + boardWidth), 50, boardWidth, 100, null);
        }

        g2d.setColor(Color.green);
        for(Pipe p : pipes){
            g2d.fillRect((int)p.x,(int)p.y,pipeWidth,pipeHeight);
            g2d.fillRect((int)p.x,(int)p.y + pipeHeight + pipeGap,pipeWidth,pipeHeight);
        }

        if(ground != null){
            g2d.drawImage(ground, (int)groundX, boardHeight-50, boardWidth, 50, null);
            g2d.drawImage(ground, (int)(groundX + boardWidth), boardHeight-50, boardWidth, 50, null);
        }

        for(Particle p : particles){
            g2d.setColor(new Color(255,255,255,(int)(p.alpha*255)));
            g2d.fillOval((int)p.x,(int)p.y,(int)p.size,(int)p.size);
        }

        AffineTransform old = g2d.getTransform();
        g2d.rotate(Math.toRadians(birdAngle), birdX + birdWidth/2, birdY + birdHeight/2);
        if(birdFrames != null && birdFrames[0] != null)
            g2d.drawImage(birdFrames[birdFrame], birdX, birdY, birdWidth, birdHeight, null);
        else {
            System.out.println("Bird image not loaded!");
            g2d.setColor(Color.red);
            g2d.fillRect(birdX, birdY, birdWidth, birdHeight);
        }
        g2d.setTransform(old);

        g2d.setColor(Color.black);
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        g2d.drawString("Score: "+score, 10, 35);

        if(state == GameState.MENU) drawMenu(g2d);
        if(state == GameState.GAMEOVER) drawGameOver(g2d);
    }

    void drawMenu(Graphics g) {
        g.setFont(new Font("Arial", Font.BOLD, 36));
        String title = "FLAPPY BIRD";
        g.drawString(title,(boardWidth-g.getFontMetrics().stringWidth(title))/2,150);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        String start = "Press ENTER to Start";
        g.drawString(start,(boardWidth-g.getFontMetrics().stringWidth(start))/2,300);
    }

    void drawGameOver(Graphics g) {
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.setColor(Color.red);
        String title = "GAME OVER";
        g.drawString(title,(boardWidth-g.getFontMetrics().stringWidth(title))/2,boardHeight/2);

        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.black);
        String restart = "Press SPACE to Restart";
        g.drawString(restart,(boardWidth-g.getFontMetrics().stringWidth(restart))/2,boardHeight/2+50);

        String hs = "High Score: "+highScore;
        g.drawString(hs,(boardWidth-g.getFontMetrics().stringWidth(hs))/2,boardHeight/2+90);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(state == GameState.RUNNING) updateGame();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch(state) {
            case MENU:
                if(e.getKeyCode() == KeyEvent.VK_ENTER) state = GameState.RUNNING;
                break;
            case RUNNING:
                if(e.getKeyCode() == KeyEvent.VK_SPACE) {
                    velocityY = jumpStrength;
                    playSound(jumpSound);
                }
                break;
            case GAMEOVER:
                if(e.getKeyCode() == KeyEvent.VK_SPACE) {
                    birdY = boardHeight/2;
                    velocityY = 0;
                    pipes.clear();
                    particles.clear();
                    score = 0;
                    state = GameState.RUNNING;
                }
                break;
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}
