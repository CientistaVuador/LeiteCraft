package cientistavuador.leitecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.joml.Vector3d;

/**
 *
 * @author Cien
 */
public class SmokeSpawner {
    
    private static class Particle {
        double x;
        double y;
        double z;
    }
    
    public static final float SPAWN_DELAY = 0.025f;
    public static final float Y_VELOCITY = 0.5f;
    
    private final Random random = new Random();
    
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final float radius;
    
    private final List<Particle> particles = new ArrayList<>();
    private double counter = 0.0;
    
    public SmokeSpawner(double centerX, double centerY, double centerZ, float radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public float getRadius() {
        return radius;
    }
    
    private void spawnSmoke() {
        float localX = 1f;
        float localY = 1f;
        float localZ = 1f;
        while (((localX * localX) + (localY * localY) + (localZ * localZ)) > 1f) {
            localX = (this.random.nextFloat() * 2f) - 1f;
            localY = (this.random.nextFloat() * 2f) - 1f;
            localZ = (this.random.nextFloat() * 2f) - 1f;
        }
        
        float invlength = (float) (1f / Math.sqrt((localX * localX) + (localY * localY) + (localZ * localZ)));
        
        localX *= this.radius * invlength;
        localY *= this.radius * invlength;
        localZ *= this.radius * invlength;
        
        Particle p = new Particle();
        
        p.x = localX + this.centerX;
        p.y = localY + this.centerY;
        p.z = localZ + this.centerZ;
        
        this.particles.add(p);
    }
    
    public void update(Camera camera) {
        this.counter += Main.TPF;
        if (this.counter >= SPAWN_DELAY) {
            this.counter = 0f;
            spawnSmoke();
        }
        
        for (Particle p:this.particles) {
            p.y = p.y + (Y_VELOCITY * Main.TPF);
        }
        
        List<Particle> toRemove = new ArrayList<>();
        for (Particle p:this.particles) {
            float px = (float) (p.x - this.centerX);
            float py = (float) (p.y - this.centerY);
            float pz = (float) (p.z - this.centerZ);
            
            float radiusSquared = (px * px) + (py * py) + (pz * pz);
            
            if (radiusSquared > (this.radius * this.radius)) {
                toRemove.add(p);
            }
        }
        
        this.particles.removeAll(toRemove);
        
        for (Particle p:this.particles) {
            float dist = (float) Vector3d.distance(
                    p.x, p.y, p.z,
                    this.centerX, this.centerY, this.centerZ
            );
            
            float lerp = dist / this.radius;
            
            float alpha = 1f - lerp;
            
            SmokeRenderer.smoke(camera,
                    p.x, p.y, p.z,
                    lerp, lerp, lerp, alpha / alpha
            );
        }
    }
    
}
