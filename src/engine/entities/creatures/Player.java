package engine.entities.creatures;

import engine.entities.Entity;
import engine.inventory.Inventory;
import engine.items.ItemManager;
import engine.tiles.Tile;
import engine.tiles.TileManager;
import engine.utils.Handler;
import engine.gfx.Animation;
import engine.gfx.Assets;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class Player extends Creature
{
    private Animation animUp, animDown, animLeft, animRight;
    // 0 = right, 1 = left, 2 = up, 3 = down
    public int direction = 0;
    private boolean attacking;
    private int reach = 4 * Tile.SIZE;
    private BufferedImage def = Assets.playerRight;
    private float startingSpeed;
    private float startingStamina;
    private float halfSpeed;
    private boolean running = false;
    private int blockType = 0;
    private Inventory inventory;
    private boolean canBuild = true;
    private boolean wall = false;

    private long lastAttackTimer, attackCooldown = 400, attackTimer = attackCooldown;

    public Player(Handler handler, float x, float y)
    {
        super(handler, x, y, Creature.DEFAULT_WIDTH, Creature.DEFAULT_HEIGHT);
        setMaxHealth(20);
        setPower(100);
        setSpeed(4);
        bounds.x = 15;
        bounds.y = 20;
        bounds.width = 20;
        bounds.height = 29;
        startingSpeed = speed;
        startingStamina = stamina;
        halfSpeed = speed / 2;

        animUp = new Animation(100, Assets.player_up);
        animDown = new Animation(100, Assets.player_down);
        animLeft = new Animation(100, Assets.player_left);
        animRight = new Animation(100, Assets.player_right);

        inventory = new Inventory(handler, false);
    }

    @Override
    public void die()
    {

    }

    @Override
    public void update()
    {
        try
        {
            getInput();
        }
        catch (IndexOutOfBoundsException e)
        {

        }

        move();
        handler.getCamera().centerOnEntity(this);

        animUp.update();
        animDown.update();
        animLeft.update();
        animRight.update();

        checkAttacks();

        if (attacking || handler.getWorld().getTileID((int) (x), (int) (y)) == TileManager.waterTile.getID())
            speed = halfSpeed;
        else
            speed = startingSpeed;

        if (x > handler.getWorld().getWidth() * Tile.SIZE)
            x = 0;
        else if (x < 0)
            x = handler.getWorld().getWidth() * Tile.SIZE;

        if (y > handler.getWorld().getHeight() * Tile.SIZE)
            y = 0;
        else if (y < 0)
            y = handler.getWorld().getHeight() * Tile.SIZE;

        inventory.update();
    }

    private void checkAttacks()
    {
        attackTimer += System.currentTimeMillis() - lastAttackTimer;
        lastAttackTimer = System.currentTimeMillis();

        if (attackTimer < attackCooldown)
        {
            attacking = true;
            return;
        }
        else
        {
            attacking = false;
        }

        if (inventory.isActive())
            return;

        Rectangle cb = getCollisionBounds(0,0);
        Rectangle ar = new Rectangle();

        int arSize = Tile.SIZE;
        ar.width = arSize;
        ar.height = arSize;

        if (handler.getKeyManager().keyJustDown(KeyEvent.VK_SPACE))
        {
            if (direction == 0)
            {
                ar.x = cb.x + cb.width;
                ar.y = cb.y + cb.height / 2 - arSize / 2;
            }
            else if (direction == 1)
            {
                ar.x = cb.x - arSize;
                ar.y = cb.y + cb.height / 2 - arSize / 2;
            }
            else if (direction == 2)
            {
                ar.x = cb.x + cb.width / 2 - arSize / 2;
                ar.y = cb.y - arSize;
            }
            else if (direction == 3)
            {
                ar.x = cb.x + cb.width / 2 - arSize / 2;
                ar.y = cb.y + cb.height;
            }
        }
        else
        {
            return;
        }

        attackTimer = 0;

        for(Entity e : handler.getWorld().getEntityManager().getEntities())
        {
            if (e.equals(this))
                continue;
            if (e.getCollisionBounds(0, 0).intersects(ar))
            {
                e.hurt(power / 2);
                return;
            }
        }
    }

    private void getInput()
    {
        xMove = 0;
        yMove = 0;

        if (inventory.isActive())
            return;

        if(handler.getKeyManager().up)
        {
            yMove = -speed;
        }
        if(handler.getKeyManager().down)
        {
            yMove = speed;
        }
        if(handler.getKeyManager().left)
        {
            xMove = -speed;
        }
        if(handler.getKeyManager().right)
        {
            xMove = speed;
        }

        if (handler.getMouseManager().isRightJustPressed())
        {
            int mouseX = (int) (handler.getMouseManager().getMouseX() + handler.getCamera().getxOffset()), mouseY = (int) (handler.getMouseManager().getMouseY() + handler.getCamera().getyOffset());
            int a = mouseY - (int) (y + height / 2);
            int b = mouseX - (int) (x + width / 2);
            int aSQ = a * a;
            int bSQ = b * b;

            int distanceToPlayer = (int) Math.sqrt(aSQ + bSQ);

            if (distanceToPlayer <= reach)
            {
                Tile posTile = handler.getWorld().getTile(mouseX / Tile.SIZE, mouseY / Tile.SIZE);
                int posTileID = posTile.getID();

                if (posTileID == TileManager.waterTile.getID() || posTileID == TileManager.lavaTile.getID())
                    return;

                if (posTileID == TileManager.woodTile.getID() || posTileID == TileManager.woodWallTile.getID())
                    inventory.addItem(ItemManager.woodItem.createNew(1));
                else if (posTileID == TileManager.stoneTile.getID() || posTileID == TileManager.stoneWallTile.getID())
                    inventory.addItem(ItemManager.rockItem.createNew(1));
                else if (posTileID == TileManager.sandTile.getID() || posTileID == TileManager.sandWallTile.getID())
                    inventory.addItem(ItemManager.sandItem.createNew(1));

                handler.getWorld().setTile(mouseX / Tile.SIZE, mouseY / Tile.SIZE, handler.getWorld().getBelowTile(mouseX / Tile.SIZE, mouseY / Tile.SIZE).getID());
                handler.getWorld().setBelowTile(mouseX / Tile.SIZE, mouseY / Tile.SIZE, handler.getWorld().getDefBelowTile(mouseX / Tile.SIZE, mouseY / Tile.SIZE).getID());
            }
        }

        if (inventory.getActiveItem() != null)
        {
            if (handler.getMouseManager().isLeftJustPressed())
            {
                int mouseX = (int) (handler.getMouseManager().getMouseX() + handler.getCamera().getxOffset()), mouseY = (int) (handler.getMouseManager().getMouseY() + handler.getCamera().getyOffset());
                int a = mouseY - (int) (y + height / 2);
                int b = mouseX - (int) (x + width / 2);
                int aSQ = a * a;
                int bSQ = b * b;

                int distanceToPlayer = (int) Math.sqrt(aSQ + bSQ);

                if (distanceToPlayer <= reach && distanceToPlayer > Tile.SIZE && handler.getWorld().getTile(mouseX / Tile.SIZE, mouseY / Tile.SIZE).getID() != blockType && canBuild)
                {
                    int posTileID = handler.getWorld().getTile(mouseX / Tile.SIZE, mouseY / Tile.SIZE).getID();

                    handler.getWorld().setTile(mouseX / Tile.SIZE, mouseY / Tile.SIZE, blockType);
                    handler.getWorld().setBelowTile(mouseX / Tile.SIZE, mouseY / Tile.SIZE, posTileID);
                    inventory.getActiveItem().setCount(inventory.getActiveItem().getCount() - 1);
                }
            }

            if (inventory.getActiveItem().getID() == ItemManager.woodItem.getID() && inventory.getActiveItem().getCount() > 0)
            {
                if (wall)
                    blockType = TileManager.woodWallTile.getID();
                else
                    blockType = TileManager.woodTile.getID();
                canBuild = true;
            }
            else if (inventory.getActiveItem().getID() == ItemManager.rockItem.getID() && inventory.getActiveItem().getCount() > 0)
            {
                if (wall)
                    blockType = TileManager.stoneWallTile.getID();
                else
                    blockType = TileManager.stoneTile.getID();
                canBuild = true;
            }
            else if (inventory.getActiveItem().getID() == ItemManager.sandItem.getID() && inventory.getActiveItem().getCount() > 0)
            {
                if (wall)
                    blockType = TileManager.sandWallTile.getID();
                else
                    blockType = TileManager.sandTile.getID();
                canBuild = true;
            }
            else if (inventory.getActiveItem().getID() == ItemManager.obsidianItem.getID() && inventory.getActiveItem().getCount() > 0)
            {
                if (wall)
                    blockType = TileManager.obsidianWallTile.getID();
                else
                    blockType = TileManager.obsidianTile.getID();
                canBuild = true;
            }

            if (inventory.getActiveItem().getCount() == 0)
                canBuild = false;
        }

        if (inventory.getActiveItem() == null)
            canBuild = false;

        if (handler.getKeyManager().keyJustDown(KeyEvent.VK_Z))
        {
            wall = !wall;
        }
    }

    @Override
    public void render(Graphics g)
    {
        if (!attacking)
            g.drawImage(getCurrentAnimationFrame(), (int) (x - handler.getCamera().getxOffset()), (int) (y - handler.getCamera().getyOffset()), width, height, null);
        else if (attacking && direction == 0)
            g.drawImage(Assets.playerRightPunch, (int) (x - handler.getCamera().getxOffset()), (int) (y - handler.getCamera().getyOffset()), width, height, null);
        else if (attacking && direction == 1)
            g.drawImage(Assets.playerLeftPunch, (int) (x - handler.getCamera().getxOffset()), (int) (y - handler.getCamera().getyOffset()), width, height, null);
        else if (attacking && direction == 2)
            g.drawImage(Assets.playerUpPunch, (int) (x - handler.getCamera().getxOffset()), (int) (y - handler.getCamera().getyOffset()), width, height, null);
        else if (attacking && direction == 3)
            g.drawImage(Assets.playerDownPunch, (int) (x - handler.getCamera().getxOffset()), (int) (y - handler.getCamera().getyOffset()), width, height, null);

    }

    public void topRender(Graphics g)
    {
        inventory.render(g);
    }

    private BufferedImage getCurrentAnimationFrame()
    {
        if (xMove > 0)
        {
            if (handler.getWorld().getTile((int)(x + xMove + bounds.x + bounds.width) / Tile.SIZE, (int)(y + yMove + bounds.y + bounds.height) / Tile.SIZE).getID() == 6)
            {
                def = Assets.playerRightSwim;
                speed = halfSpeed;
                direction = 0;
                return Assets.playerRightSwim;
            }
            else
            {
                def = Assets.playerRight;
                speed = startingSpeed;
                direction = 0;
                return animRight.getCurrentFrame();
            }
        }
        else if (xMove < 0)
        {
            if (handler.getWorld().getTile((int)(x + xMove + bounds.x + bounds.width) / Tile.SIZE, (int)(y + yMove + bounds.y + bounds.height) / Tile.SIZE).getID() == 6)
            {
                def = Assets.playerLeftSwim;
                speed = halfSpeed;
                direction = 1;
                return Assets.playerLeftSwim;
            }
            else
            {
                def = Assets.playerLeft;
                speed = startingSpeed;
                direction = 1;
                return animLeft.getCurrentFrame();
            }
        }
        else if (yMove < 0)
        {
            if (handler.getWorld().getTile((int)(x + xMove + bounds.x + bounds.width) / Tile.SIZE, (int)(y + yMove + bounds.y + bounds.height) / Tile.SIZE).getID() == 6)
            {
                def = Assets.playerUpSwim;
                speed = halfSpeed;
                direction = 2;
                return Assets.playerUpSwim;
            }
            else
            {
                def = Assets.playerUp;
                speed = startingSpeed;
                direction = 2;
                return animUp.getCurrentFrame();
            }
        }
        else if (yMove > 0)
        {
            if (handler.getWorld().getTile((int)(x + xMove + bounds.x + bounds.width) / Tile.SIZE, (int)(y + yMove + bounds.y + bounds.height) / Tile.SIZE).getID() == 6)
            {
                def = Assets.playerDownSwim;
                speed = halfSpeed;
                direction = 3;
                return Assets.playerDownSwim;
            }
            else
            {
                def = Assets.playerDown;
                speed = startingSpeed;
                direction = 3;
                return animDown.getCurrentFrame();
            }
        }
        else
        {
            return def;
        }
    }

    public Inventory getInventory()
    {
        return inventory;
    }

    @Override
    protected void ai()
    {
        // player is controlled by us, so ai is unnecessary
    }
}
