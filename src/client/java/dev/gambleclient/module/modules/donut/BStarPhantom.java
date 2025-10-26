package dev.gambleclient.module.modules.donut;

import dev.gambleclient.Gamble;
import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.modules.ai.DirectionalPathfinder;
import dev.gambleclient.module.modules.ai.MiningState;
import dev.gambleclient.module.modules.ai.ParkourHelper;
import dev.gambleclient.module.modules.ai.PathScanner;
import dev.gambleclient.module.modules.ai.RotationController;
import dev.gambleclient.module.modules.ai.SafetyValidator;
import dev.gambleclient.module.modules.ai.SimpleSneakCentering;
import dev.gambleclient.module.modules.misc.AutoEat;
import dev.gambleclient.module.setting.BlockEntityListSetting;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.ModeSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.MathHelper;
import net.minecraft.fluid.Fluids;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public class BStarPhantom extends Module {
   private final NumberSetting scanDepth = new NumberSetting(EncryptedString.of("Scan Depth"), (double)10.0F, (double)30.0F, (double)20.0F, (double)1.0F);
   private final BooleanSetting humanLikeRotation = new BooleanSetting(EncryptedString.of("Human-like Rotation"), true);
   private final BooleanSetting disconnectOnBaseFind = new BooleanSetting(EncryptedString.of("Log On Base/Spawners"), true);
   private final BooleanSetting showHazards = new BooleanSetting(EncryptedString.of("Show Hazards"), true);
   private final NumberSetting baseThreshold = new NumberSetting(EncryptedString.of("Min Storage For Base"), (double)1.0F, (double)20.0F, (double)4.0F, (double)1.0F);
   private static final List DEFAULT_STORAGE;
   private final BlockEntityListSetting storageBlocks;
   private final BooleanSetting debugMode;
   private final BooleanSetting showWaypoints;
   private final NumberSetting yLevel;
   private final NumberSetting scanHeight;
   private final NumberSetting scanWidthFalling;
   private final NumberSetting scanWidthFluids;
   private final BooleanSetting strictGroundCheck;
   private final BooleanSetting smoothRotation;
   private final NumberSetting rotationSpeed;
   private final NumberSetting rotationAcceleration;
   private final NumberSetting overshootChance;
   private final BooleanSetting workInMenus;
   private final BooleanSetting pauseForAutoEat;
   private final BooleanSetting rtpWhenStuck;
   private final ModeSetting rtpRegion;
   private final NumberSetting mineDownTarget;
   private final NumberSetting groundScanSize;
   private final BooleanSetting autoEnableAutoLog;
   private final NumberSetting safeMineDownSearchRadius;
   private final BooleanSetting autoRepair;
   private final NumberSetting repairThreshold;
   private final NumberSetting repairDuration;
   private final BooleanSetting detectPlayers;
   private final NumberSetting voidDepthThreshold;
   private final NumberSetting playerCheckInterval;
   private final NumberSetting rotationRandomness;
   private final BooleanSetting alwaysFocused;
   private final NumberSetting retoggleDelay;
   private final BooleanSetting bypassMiningDown;
   private final NumberSetting intervalHoldDuration;
   private final NumberSetting intervalPauseDuration;
   private MiningState currentState;
   private DirectionalPathfinder pathfinder;
   private PathScanner pathScanner;
   private SafetyValidator safetyValidator;
   private RotationController rotationController;
   private SimpleSneakCentering centeringHelper;
   private ParkourHelper parkourHelper;
   private boolean isDetouring;
   private BlockPos currentWaypoint;
   private PathScanner.ScanResult lastHazardDetected;
   private Direction pendingDirection;
   private int tickCounter;
   private int playerCheckTicks;
   private long moduleStartTime;
   private final Set hazardBlocks;
   private final Set waypointBlocks;
   private final Set processedChunks;
   private int blocksMined;
   private int totalBlocksMined;
   private Vec3d lastPos;
   private int scanTicks;
   private long lastRtpTime;
   private int rtpWaitTicks;
   private int rtpAttempts;
   private BlockPos preRtpPos;
   private boolean rtpCommandSent;
   private boolean hasReachedMineDepth;
   private int mineDownScanTicks;
   private boolean inRTPRecovery;
   private boolean isThrowingExp;
   private MiningState stateBeforeExp;
   private int expThrowTicks;
   private long lastExpThrowTime;
   private boolean wasPausedForEating;
   private MiningState stateBeforeEating;
   private boolean miningDownInitiated;
   private int miningDownToggleTicks;
   private BlockPos safeMineDownTarget;
   private int searchAttempts;
   private boolean searchingForSafeMineSpot;
   private boolean waitingAtTargetDepth;
   private int targetDepthWaitTicks;
   private BlockPos moveAndRetryTarget;
   private int moveAndRetryTicks;
   private boolean waitingToRetoggle;
   private int retoggleDelayTicks;
   private boolean intervalMiningActive;
   private boolean intervalHolding;
   private long intervalStartTime;
   private int retoggleAttempts;
   private static final int SCAN_INTERVAL = 20;
   private static final int RTP_COOLDOWN_SECONDS = 16;
   private static final int RTP_WAIT_TICKS = 200;
   private static final int MINE_DOWN_SCAN_INTERVAL = 2;
   private static final int TARGET_DEPTH_WAIT_DURATION = 30;
   private static final int MOVE_RETRY_DURATION = 20;
   private static final int MAX_SEARCH_ATTEMPTS = 5;
   private static final int MAX_RETOGGLE_ATTEMPTS = 1;
   private static final Pattern RTP_COOLDOWN_PATTERN;
   private static final long EXP_THROW_COOLDOWN = 10000L;

   public BStarPhantom() {
      super(EncryptedString.of("BStar"), EncryptedString.of("(Beta) Directional mining with pathfinding & RTP recovery"), -1, Category.DONUT);
      this.storageBlocks = new BlockEntityListSetting(EncryptedString.of("Storage Blocks"), DEFAULT_STORAGE);
      this.debugMode = new BooleanSetting(EncryptedString.of("Debug Mode"), true);
      this.showWaypoints = new BooleanSetting(EncryptedString.of("Show Waypoints"), true);
      this.yLevel = new NumberSetting(EncryptedString.of("Max Y"), (double)-64.0F, (double)-10.0F, (double)-20.0F, (double)1.0F);
      this.scanHeight = new NumberSetting(EncryptedString.of("Scan Height"), (double)3.0F, (double)8.0F, (double)4.0F, (double)1.0F);
      this.scanWidthFalling = new NumberSetting(EncryptedString.of("Falling Width"), (double)1.0F, (double)4.0F, (double)1.0F, (double)1.0F);
      this.scanWidthFluids = new NumberSetting(EncryptedString.of("Fluids Width"), (double)2.0F, (double)6.0F, (double)2.0F, (double)1.0F);
      this.strictGroundCheck = new BooleanSetting(EncryptedString.of("Strict Ground"), false);
      this.smoothRotation = new BooleanSetting(EncryptedString.of("Smooth Rotation"), true);
      this.rotationSpeed = new NumberSetting(EncryptedString.of("Rotation Speed"), (double)1.0F, (double)10.0F, (double)4.5F, 0.1);
      this.rotationAcceleration = new NumberSetting(EncryptedString.of("Rotation Accel"), 0.1, (double)2.0F, 0.8, 0.05);
      this.overshootChance = new NumberSetting(EncryptedString.of("Overshoot Chance"), (double)0.0F, (double)1.0F, 0.3, 0.05);
      this.workInMenus = new BooleanSetting(EncryptedString.of("Work In Menus"), false);
      this.pauseForAutoEat = new BooleanSetting(EncryptedString.of("Pause For AutoEat"), true);
      this.rtpWhenStuck = new BooleanSetting(EncryptedString.of("RTP When Stuck"), false);
      this.rtpRegion = new ModeSetting(EncryptedString.of("RTP Region"), BStarPhantom.RTPRegion.EU_CENTRAL, RTPRegion.class);
      this.mineDownTarget = new NumberSetting(EncryptedString.of("Mine Down Target Y"), (double)-64.0F, (double)-10.0F, (double)-30.0F, (double)1.0F);
      this.groundScanSize = new NumberSetting(EncryptedString.of("Ground Scan Size"), (double)3.0F, (double)9.0F, (double)5.0F, (double)1.0F);
      this.autoEnableAutoLog = new BooleanSetting(EncryptedString.of("Auto Enable AutoLog"), false);
      this.safeMineDownSearchRadius = new NumberSetting(EncryptedString.of("Safe MineDown Radius"), (double)5.0F, (double)20.0F, (double)10.0F, (double)1.0F);
      this.autoRepair = new BooleanSetting(EncryptedString.of("Auto Repair"), false);
      this.repairThreshold = new NumberSetting(EncryptedString.of("Repair Threshold %"), (double)10.0F, (double)50.0F, (double)20.0F, (double)1.0F);
      this.repairDuration = new NumberSetting(EncryptedString.of("Repair Duration s"), (double)1.0F, (double)5.0F, (double)3.0F, (double)1.0F);
      this.detectPlayers = new BooleanSetting(EncryptedString.of("Detect Players"), true);
      this.voidDepthThreshold = new NumberSetting(EncryptedString.of("Void Depth Threshold"), (double)5.0F, (double)15.0F, (double)7.0F, (double)1.0F);
      this.playerCheckInterval = new NumberSetting(EncryptedString.of("Player Check Interval"), (double)10.0F, (double)60.0F, (double)20.0F, (double)1.0F);
      this.rotationRandomness = new NumberSetting(EncryptedString.of("Rotation Randomness"), (double)0.0F, (double)2.0F, (double)0.5F, 0.05);
      this.alwaysFocused = new BooleanSetting(EncryptedString.of("Always Focused"), false);
      this.retoggleDelay = new NumberSetting(EncryptedString.of("Retoggle Delay s"), (double)0.0F, (double)6.0F, (double)3.0F, (double)1.0F);
      this.bypassMiningDown = new BooleanSetting(EncryptedString.of("Bypass Mining Down"), false);
      this.intervalHoldDuration = new NumberSetting(EncryptedString.of("Interval Hold ms"), (double)200.0F, (double)1500.0F, (double)1000.0F, (double)20.0F);
      this.intervalPauseDuration = new NumberSetting(EncryptedString.of("Interval Pause ms"), (double)400.0F, (double)1500.0F, (double)460.0F, (double)20.0F);
      this.currentState = MiningState.IDLE;
      this.isDetouring = false;
      this.tickCounter = 0;
      this.playerCheckTicks = 0;
      this.moduleStartTime = 0L;
      this.hazardBlocks = new HashSet();
      this.waypointBlocks = new HashSet();
      this.processedChunks = new HashSet();
      this.blocksMined = 0;
      this.totalBlocksMined = 0;
      this.lastPos = Vec3d.ZERO;
      this.scanTicks = 0;
      this.lastRtpTime = 0L;
      this.rtpWaitTicks = 0;
      this.rtpAttempts = 0;
      this.preRtpPos = null;
      this.rtpCommandSent = false;
      this.hasReachedMineDepth = false;
      this.mineDownScanTicks = 0;
      this.inRTPRecovery = false;
      this.isThrowingExp = false;
      this.stateBeforeExp = MiningState.IDLE;
      this.expThrowTicks = 0;
      this.lastExpThrowTime = 0L;
      this.wasPausedForEating = false;
      this.stateBeforeEating = MiningState.IDLE;
      this.miningDownInitiated = false;
      this.miningDownToggleTicks = 0;
      this.safeMineDownTarget = null;
      this.searchAttempts = 0;
      this.searchingForSafeMineSpot = false;
      this.waitingAtTargetDepth = false;
      this.targetDepthWaitTicks = 0;
      this.moveAndRetryTarget = null;
      this.moveAndRetryTicks = 0;
      this.waitingToRetoggle = false;
      this.retoggleDelayTicks = 0;
      this.intervalMiningActive = false;
      this.intervalHolding = false;
      this.intervalStartTime = 0L;
      this.retoggleAttempts = 0;
      this.addSettings(new Setting[]{this.scanDepth, this.humanLikeRotation, this.disconnectOnBaseFind, this.showHazards, this.baseThreshold, this.storageBlocks, this.debugMode, this.showWaypoints, this.yLevel, this.scanHeight, this.scanWidthFalling, this.scanWidthFluids, this.strictGroundCheck, this.smoothRotation, this.rotationSpeed, this.rotationAcceleration, this.overshootChance, this.workInMenus, this.pauseForAutoEat, this.rtpWhenStuck, this.rtpRegion, this.mineDownTarget, this.groundScanSize, this.autoEnableAutoLog, this.safeMineDownSearchRadius, this.autoRepair, this.repairThreshold, this.repairDuration, this.detectPlayers, this.voidDepthThreshold, this.playerCheckInterval, this.rotationRandomness, this.alwaysFocused, this.retoggleDelay, this.bypassMiningDown, this.intervalHoldDuration, this.intervalPauseDuration});
   }

   public void onEnable() {
      if (this.mc.player == null) {
         this.toggle(false);
      } else {
         this.moduleStartTime = System.currentTimeMillis();
         this.pathScanner = new PathScanner();
         this.pathScanner.updateTunnelDimensions(3, 3);
         this.pathfinder = new DirectionalPathfinder(this.pathScanner);
         this.safetyValidator = new SafetyValidator();
         this.rotationController = new RotationController();
         this.rotationController.setPreciseLanding(false);
         this.centeringHelper = new SimpleSneakCentering();
         this.parkourHelper = new ParkourHelper();
         this.hazardBlocks.clear();
         this.waypointBlocks.clear();
         this.processedChunks.clear();
         this.blocksMined = 0;
         this.totalBlocksMined = 0;
         this.lastPos = this.mc.player.getPos();
         this.scanTicks = 0;
         this.tickCounter = 0;
         this.playerCheckTicks = 0;
         this.lastRtpTime = 0L;
         this.rtpWaitTicks = 0;
         this.rtpAttempts = 0;
         this.preRtpPos = null;
         this.rtpCommandSent = false;
         this.hasReachedMineDepth = false;
         this.mineDownScanTicks = 0;
         this.inRTPRecovery = false;
         this.safeMineDownTarget = null;
         this.searchAttempts = 0;
         this.searchingForSafeMineSpot = false;
         this.waitingAtTargetDepth = false;
         if (this.mc.player.getY() > (double)this.yLevel.getIntValue()) {
            this.currentState = MiningState.RTP_SCANNING_GROUND;
            this.inRTPRecovery = true;
         } else {
            this.currentState = MiningState.CENTERING;
         }

      }
   }

   public void onDisable() {
      this.releaseAllMovement();
      this.hazardBlocks.clear();
      this.waypointBlocks.clear();
      this.processedChunks.clear();
      this.inRTPRecovery = false;
      this.currentState = MiningState.IDLE;
      if (this.parkourHelper != null) {
         this.parkourHelper.reset();
      }

      if (this.safetyValidator != null) {
         this.safetyValidator.reset();
      }

   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.isEnabled() && this.mc.player != null) {
         ++this.tickCounter;
         this.rotationController.update();
         if (this.parkourHelper != null) {
            this.parkourHelper.update();
         }

         if (!this.workInMenus.getValue() && this.mc.currentScreen != null) {
            this.releaseAllMovement();
         } else {
            if (this.pauseForAutoEat.getValue()) {
               Module eatMod = Gamble.INSTANCE.getModuleManager().getModuleByClass(AutoEat.class);
               if (eatMod instanceof AutoEat) {
                  AutoEat eat = (AutoEat)eatMod;
                  if (eatMod.isEnabled()) {
                     if (eat.shouldEat()) {
                        if (!this.wasPausedForEating) {
                           this.wasPausedForEating = true;
                           this.stateBeforeEating = this.currentState;
                           this.releaseAllMovement();
                           this.currentState = MiningState.IDLE;
                        }

                        return;
                     }

                     if (this.wasPausedForEating) {
                        this.wasPausedForEating = false;
                        this.currentState = this.stateBeforeEating;
                     }
                  }
               }
            }

            if (this.detectPlayers.getValue()) {
               ++this.playerCheckTicks;
               if (this.playerCheckTicks >= this.playerCheckInterval.getIntValue()) {
                  this.playerCheckTicks = 0;
                  this.checkForPlayers();
               }
            }

            if (this.autoRepair.getValue() && !this.isThrowingExp && !this.isInRTPState()) {
               long since = System.currentTimeMillis() - this.lastExpThrowTime;
               if ((since >= 10000L || this.lastExpThrowTime == 0L) && this.shouldRepairNow()) {
                  this.startRepairSequence();
               }
            }

            if (this.isThrowingExp) {
               this.handleExpRepairTick();
            } else {
               this.pathScanner.updateScanWidths(this.scanWidthFalling.getIntValue(), this.scanWidthFluids.getIntValue());
               this.rotationController.updateSettings(this.smoothRotation.getValue(), this.rotationSpeed.getValue(), this.rotationAcceleration.getValue(), this.humanLikeRotation.getValue(), this.overshootChance.getValue());
               this.rotationController.updateRandomVariation(this.rotationRandomness.getValue());
               if (!this.isInRTPState() && !this.safetyValidator.canContinue(this.mc.player, this.yLevel.getIntValue())) {
                  this.toggle(false);
               } else if (!this.rotationController.isRotating()) {
                  try {
                     switch (this.currentState) {
                        case CENTERING:
                           this.handleCentering();
                           break;
                        case SCANNING_PRIMARY:
                           this.handleScanningPrimary();
                           break;
                        case MINING_PRIMARY:
                           this.handleMiningPrimary();
                           break;
                        case HAZARD_DETECTED:
                           this.handleHazardDetected();
                           break;
                        case CALCULATING_DETOUR:
                           this.handleCalculatingDetour();
                           break;
                        case FOLLOWING_DETOUR:
                           this.handleFollowingDetour();
                           break;
                        case CHANGING_DIRECTION:
                           this.handleChangingDirection();
                        case ROTATING:
                        default:
                           break;
                        case RTP_INITIATED:
                           this.handleRTPInitiated();
                           break;
                        case RTP_WAITING:
                           this.handleRTPWaiting();
                           break;
                        case RTP_COOLDOWN:
                           this.handleRTPCooldown();
                           break;
                        case RTP_SCANNING_GROUND:
                           this.handleRTPScanningGround();
                           break;
                        case MINING_DOWN:
                           this.handleMiningDown();
                           break;
                        case SEARCHING_SAFE_MINEDOWN:
                           this.handleSearchingSafeMinedown();
                           break;
                        case MOVING_TO_MINEDOWN:
                           this.handleMovingToMinedown();
                           break;
                        case STOPPED:
                           this.toggle(false);
                     }
                  } catch (Exception ex) {
                     if (this.debugMode.getValue()) {
                        ex.printStackTrace();
                     }

                     this.toggle(false);
                  }

               }
            }
         }
      }
   }

   private void handleCentering() {
      if (!this.centeringHelper.isCentering() && !this.centeringHelper.startCentering()) {
         this.proceedAfterCentering();
      } else {
         if (!this.centeringHelper.tick()) {
            this.proceedAfterCentering();
         }

      }
   }

   private void proceedAfterCentering() {
      if (this.safeMineDownTarget != null && this.mc.player.getBlockPos().equals(this.safeMineDownTarget)) {
         this.inRTPRecovery = true;
         this.safeMineDownTarget = null;
         this.currentState = MiningState.RTP_SCANNING_GROUND;
      } else if (this.inRTPRecovery && this.mc.player.getY() > this.mineDownTarget.getValue()) {
         this.currentState = MiningState.RTP_SCANNING_GROUND;
      } else {
         Direction initialDir = this.yawToCardinal(this.mc.player.getYaw());
         this.pathfinder.setInitialDirection(initialDir);
         float targetYaw = this.directionToYaw(initialDir);
         this.currentState = MiningState.ROTATING;
         this.rotationController.startRotation(targetYaw, 0.0F, () -> this.currentState = MiningState.SCANNING_PRIMARY);
      }
   }

   private void handleScanningPrimary() {
      BlockPos playerPos = this.mc.player.getBlockPos();
      Direction dir = this.pathfinder.getPrimaryDirection();
      PathScanner.ScanResult result = this.pathScanner.scanDirection(playerPos, dir, this.scanDepth.getIntValue(), this.scanHeight.getIntValue(), this.strictGroundCheck.getValue());
      if (result.isSafe()) {
         this.hazardBlocks.clear();
         this.currentState = MiningState.MINING_PRIMARY;
         this.scanTicks = 0;
         this.lastPos = this.mc.player.getPos();
         this.startMiningForward();
      } else {
         this.lastHazardDetected = result;
         this.currentState = MiningState.HAZARD_DETECTED;
         if (this.showHazards.getValue()) {
            this.hazardBlocks.clear();
            this.hazardBlocks.addAll(result.getHazardPositions());
         }
      }

   }

   private void handleMiningPrimary() {
      Vec3d currentPos = this.mc.player.getPos();
      ++this.scanTicks;
      if (this.scanTicks >= 20) {
         this.scanTicks = 0;
         BlockPos playerPos = this.mc.player.getBlockPos();
         Direction dir = this.pathfinder.getPrimaryDirection();
         PathScanner.ScanResult result = this.pathScanner.scanDirection(playerPos, dir, this.scanDepth.getIntValue(), this.scanHeight.getIntValue(), false);
         if (!result.isSafe()) {
            this.stopMiningForward();
            this.lastHazardDetected = result;
            this.currentState = MiningState.HAZARD_DETECTED;
            if (this.showHazards.getValue()) {
               this.hazardBlocks.clear();
               this.hazardBlocks.addAll(result.getHazardPositions());
            }

            return;
         }
      }

      double dist = currentPos.distanceTo(this.lastPos);
      if (dist >= 0.8) {
         ++this.blocksMined;
         ++this.totalBlocksMined;
         this.lastPos = currentPos;
      }

   }

   private void handleHazardDetected() {
      this.stopMiningForward();
      this.currentState = MiningState.CALCULATING_DETOUR;
   }

   private void handleCalculatingDetour() {
      BlockPos pos = this.mc.player.getBlockPos();
      DirectionalPathfinder.PathPlan plan = this.pathfinder.calculateDetour(pos, this.lastHazardDetected);
      if (plan.newPrimaryDirection != null) {
         this.pendingDirection = plan.newPrimaryDirection;
         this.currentState = MiningState.CHANGING_DIRECTION;
      } else if (plan.needsDetour) {
         this.currentWaypoint = this.pathfinder.getNextWaypoint();
         this.isDetouring = true;
         this.currentState = MiningState.FOLLOWING_DETOUR;
         if (this.showWaypoints.getValue()) {
            this.waypointBlocks.clear();
            if (this.currentWaypoint != null) {
               this.waypointBlocks.add(this.currentWaypoint);
            }

            this.waypointBlocks.addAll(this.pathfinder.peekAllWaypoints());
         }
      } else if (plan.reason != null && plan.reason.contains("No valid paths")) {
         if (this.rtpWhenStuck.getValue()) {
            this.initiateRTP();
         } else {
            this.currentState = MiningState.STOPPED;
         }
      } else {
         this.currentState = MiningState.STOPPED;
      }

   }

   private void handleFollowingDetour() {
      if (this.currentWaypoint == null) {
         this.currentWaypoint = this.pathfinder.getNextWaypoint();
         if (this.currentWaypoint == null) {
            this.pathfinder.completeDetour();
            this.isDetouring = false;
            this.currentState = MiningState.SCANNING_PRIMARY;
            this.waypointBlocks.clear();
            return;
         }

         if (this.showWaypoints.getValue()) {
            this.waypointBlocks.clear();
            this.waypointBlocks.add(this.currentWaypoint);
            this.waypointBlocks.addAll(this.pathfinder.peekAllWaypoints());
         }
      }

      BlockPos playerPos = this.mc.player.getBlockPos();
      double distance = Math.sqrt(Math.pow((double)(playerPos.getX() - this.currentWaypoint.getX()), (double)2.0F) + Math.pow((double)(playerPos.getZ() - this.currentWaypoint.getZ()), (double)2.0F));
      if (distance < (double)0.5F) {
         this.currentWaypoint = null;
      } else {
         Direction dirToWaypoint = this.getDirectionToward(playerPos, this.currentWaypoint);
         if (dirToWaypoint == null) {
            int dx = this.currentWaypoint.getX() - playerPos.getX();
            int dz = this.currentWaypoint.getZ() - playerPos.getZ();
            if (Math.abs(dx) > Math.abs(dz)) {
               dirToWaypoint = dx > 0 ? Direction.EAST : Direction.WEST;
            } else {
               if (dz == 0) {
                  this.currentWaypoint = null;
                  return;
               }

               dirToWaypoint = dz > 0 ? Direction.SOUTH : Direction.NORTH;
            }
         }

         float currentYaw = this.mc.player.getYaw();
         float targetYaw = this.directionToYaw(dirToWaypoint);
         float diff = Math.abs(MathHelper.wrapDegrees(currentYaw - targetYaw));
         if (diff > 15.0F) {
            this.stopMiningForward();
            this.currentState = MiningState.ROTATING;
            this.rotationController.startRotation(targetYaw, 0.0F, () -> this.currentState = MiningState.FOLLOWING_DETOUR);
         } else {
            this.startMiningForward();
            this.lastPos = this.mc.player.getPos();
            ++this.scanTicks;
            if (this.scanTicks >= 20) {
               this.scanTicks = 0;
               PathScanner.ScanResult quick = this.pathScanner.scanDirection(playerPos, dirToWaypoint, 3, 4, false);
               if (!quick.isSafe() && quick.getHazardDistance() <= 2) {
                  this.stopMiningForward();
                  this.currentWaypoint = null;
                  this.pathfinder.completeDetour();
                  this.lastHazardDetected = quick;
                  this.currentState = MiningState.CALCULATING_DETOUR;
               }
            }

         }
      }
   }

   private void handleChangingDirection() {
      if (this.rotationController.isRotating()) {
         this.stopMiningForward();
      } else if (this.pendingDirection == null) {
         if (this.rtpWhenStuck.getValue()) {
            this.initiateRTP();
         } else {
            this.currentState = MiningState.STOPPED;
         }

      } else {
         if (!this.centeringHelper.isCentering()) {
            if (!this.isCenteredOnBlock(this.mc.player.getBlockPos()) && this.centeringHelper.startCentering()) {
               return;
            }
         } else if (this.centeringHelper.tick()) {
            return;
         }

         float targetYaw = this.directionToYaw(this.pendingDirection);
         this.currentState = MiningState.ROTATING;
         this.rotationController.startRotation(targetYaw, 0.0F, () -> {
            this.pathfinder.setInitialDirection(this.pendingDirection);
            this.currentState = MiningState.SCANNING_PRIMARY;
            this.pendingDirection = null;
         });
      }
   }

   private void handleRTPInitiated() {
      ++this.rtpWaitTicks;
      if (this.rtpWaitTicks > 20) {
         this.currentState = MiningState.RTP_WAITING;
         this.rtpWaitTicks = 0;
      }

   }

   private void handleRTPWaiting() {
      ++this.rtpWaitTicks;
      if (this.rtpWaitTicks > 200) {
         if (this.rtpAttempts < 3) {
            this.initiateRTP();
         } else {
            this.toggle(false);
         }
      }

   }

   private void handleRTPCooldown() {
      ++this.rtpWaitTicks;
      if (this.rtpWaitTicks >= 320) {
         this.initiateRTP();
      }

   }

   private void handleRTPScanningGround() {
      this.releaseForwardAttack();
      if (this.mc.player.getY() <= this.mineDownTarget.getValue()) {
         this.hasReachedMineDepth = true;
         this.rtpAttempts = 0;
         this.inRTPRecovery = false;
         this.currentState = MiningState.CENTERING;
      } else {
         if (!this.centeringHelper.isCentering() && !this.isCenteredOnBlock(this.mc.player.getBlockPos())) {
            if (this.centeringHelper.startCentering()) {
               return;
            }
         } else if (this.centeringHelper.isCentering() && this.centeringHelper.tick()) {
            return;
         }

         float pitch = this.mc.player.getPitch();
         if (Math.abs(pitch - 90.0F) > 1.0F) {
            this.currentState = MiningState.ROTATING;
            this.rotationController.setPreciseLanding(true);
            this.rotationController.startRotation(this.mc.player.getYaw(), 90.0F, () -> {
               this.rotationController.setPreciseLanding(false);
               this.currentState = MiningState.RTP_SCANNING_GROUND;
            });
         } else {
            GroundHazard hazard = this.scanGroundBelowDetailed(this.mc.player.getBlockPos());
            if (hazard == BStarPhantom.GroundHazard.NONE) {
               this.hasReachedMineDepth = false;
               this.mineDownScanTicks = 0;
               this.miningDownInitiated = false;
               this.miningDownToggleTicks = 0;
               this.currentState = MiningState.MINING_DOWN;
            } else {
               if (this.safeMineDownTarget == null || !this.mc.player.getBlockPos().equals(this.safeMineDownTarget)) {
                  this.searchAttempts = 0;
                  this.safeMineDownTarget = null;
               }

               this.currentState = MiningState.SEARCHING_SAFE_MINEDOWN;
            }

         }
      }
   }

   private void handleMiningDown() {
      if (!this.waitingToRetoggle) {
         if (this.mc.player.getY() <= this.mineDownTarget.getValue()) {
            if (!this.hasReachedMineDepth && !this.waitingAtTargetDepth) {
               this.releaseForwardAttack();
               this.waitingAtTargetDepth = true;
               this.targetDepthWaitTicks = 0;
               return;
            }

            if (this.waitingAtTargetDepth) {
               ++this.targetDepthWaitTicks;
               if (this.targetDepthWaitTicks >= 30) {
                  this.hasReachedMineDepth = true;
                  this.waitingAtTargetDepth = false;
                  this.targetDepthWaitTicks = 0;
                  this.rtpAttempts = 0;
                  this.inRTPRecovery = false;
                  this.rtpCommandSent = false;
                  this.miningDownInitiated = false;
                  this.safeMineDownTarget = null;
                  this.searchAttempts = 0;
                  this.intervalMiningActive = false;
                  this.intervalHolding = false;
                  this.retoggleAttempts = 0;
                  this.currentState = MiningState.CENTERING;
               }

               return;
            }
         }

         if (!this.miningDownInitiated) {
            this.releaseForwardAttack();
            this.miningDownInitiated = true;
            this.miningDownToggleTicks = 0;
            this.intervalMiningActive = false;
            this.intervalHolding = false;
         } else {
            ++this.miningDownToggleTicks;
            ++this.mineDownScanTicks;
            if (this.mineDownScanTicks >= 2) {
               this.mineDownScanTicks = 0;
               BlockPos currentPos = this.mc.player.getBlockPos();
               BlockPos predicted = currentPos.down(5);
               GroundHazard hazard = this.scanGroundBelowDetailed(predicted);
               if (hazard != BStarPhantom.GroundHazard.NONE) {
                  this.releaseForwardAttack();
                  this.intervalMiningActive = false;
                  this.intervalHolding = false;
                  this.currentState = MiningState.SEARCHING_SAFE_MINEDOWN;
                  return;
               }
            }

            if (this.miningDownToggleTicks > 40 && this.miningDownToggleTicks % 40 == 0) {
               Vec3d currentPos = this.mc.player.getPos();
               if (this.lastPos != null && Math.abs(currentPos.y - this.lastPos.y) < (double)0.5F) {
                  if (this.retoggleAttempts >= 1) {
                     this.releaseForwardAttack();
                     this.intervalMiningActive = false;
                     this.intervalHolding = false;
                     this.currentState = MiningState.SEARCHING_SAFE_MINEDOWN;
                     ++this.searchAttempts;
                     if (this.searchAttempts >= 5) {
                        if (this.rtpWhenStuck.getValue()) {
                           this.rtpAttempts = 0;
                           this.searchAttempts = 0;
                           this.initiateRTP();
                        } else {
                           this.toggle(false);
                        }
                     }

                     return;
                  }

                  this.releaseForwardAttack();
                  this.intervalMiningActive = false;
                  this.intervalHolding = false;
                  ++this.retoggleAttempts;
                  this.waitingToRetoggle = true;
                  this.retoggleDelayTicks = 0;
                  return;
               }

               if (this.retoggleAttempts > 0) {
                  this.retoggleAttempts = 0;
               }

               this.lastPos = currentPos;
            }

            if (this.bypassMiningDown.getValue()) {
               this.handleIntervalMining();
            } else if (!this.waitingToRetoggle) {
               this.setKey(this.mc.options.attackKey, true);
               this.setKey(this.mc.options.sneakKey, true);
            }

         }
      }
   }

   private void handleSearchingSafeMinedown() {
      this.inRTPRecovery = true;
      this.releaseForwardAttack();
      BlockPos playerPos = this.mc.player.getBlockPos();
      BlockPos safeSpot = this.findSafeMineDownSpot(playerPos);
      if (safeSpot != null) {
         this.safeMineDownTarget = safeSpot;
         List<BlockPos> path = this.createPathToMineDown(playerPos, safeSpot);
         if (!path.isEmpty()) {
            Queue<BlockPos> queue = new LinkedList(path);
            this.pathfinder.currentDetour = new LinkedList(queue);
            this.pathfinder.isDetouring = true;
            this.currentWaypoint = this.pathfinder.getNextWaypoint();
            this.currentState = MiningState.MOVING_TO_MINEDOWN;
            if (this.showWaypoints.getValue()) {
               this.waypointBlocks.clear();
               this.waypointBlocks.addAll(path);
            }
         } else {
            this.initiateRTPFallback();
         }
      } else {
         this.initiateRTPFallback();
      }

   }

   private void handleMovingToMinedown() {
      this.inRTPRecovery = true;
      if (this.rotationController.isRotating()) {
         this.releaseForwardAttack();
      } else {
         if (this.currentWaypoint == null) {
            this.currentWaypoint = this.pathfinder.getNextWaypoint();
            if (this.currentWaypoint == null) {
               if (this.pathfinder != null && this.pathfinder.currentDetour != null) {
                  this.pathfinder.currentDetour.clear();
                  this.pathfinder.isDetouring = false;
               }

               this.waypointBlocks.clear();
               this.currentState = MiningState.CENTERING;
               return;
            }
         }

         BlockPos playerPos = this.mc.player.getBlockPos();
         Direction dirToWaypoint = this.getDirectionToward(playerPos, this.currentWaypoint);
         if (dirToWaypoint != null) {
            float currentYaw = this.mc.player.getYaw();
            float currentPitch = this.mc.player.getPitch();
            float targetYaw = this.directionToYaw(dirToWaypoint);
            float diff = Math.abs(MathHelper.wrapDegrees(currentYaw - targetYaw));
            boolean needsPitchReset = Math.abs(currentPitch) > 10.0F;
            if (diff > 15.0F || needsPitchReset) {
               this.releaseForwardAttack();
               this.currentState = MiningState.ROTATING;
               this.rotationController.startRotation(targetYaw, 0.0F, () -> this.currentState = MiningState.MOVING_TO_MINEDOWN);
               return;
            }
         }

         double dist = Math.sqrt(Math.pow((double)(playerPos.getX() - this.currentWaypoint.getX()), (double)2.0F) + Math.pow((double)(playerPos.getZ() - this.currentWaypoint.getZ()), (double)2.0F));
         if (dist < (double)0.5F) {
            this.currentWaypoint = null;
         } else {
            if (dirToWaypoint != null && !this.rotationController.isRotating()) {
               this.startMiningForward();
               this.lastPos = this.mc.player.getPos();
               ++this.scanTicks;
               if (this.scanTicks >= 20) {
                  this.scanTicks = 0;
                  PathScanner.ScanResult quick = this.pathScanner.scanDirection(playerPos, dirToWaypoint, 3, this.scanHeight.getIntValue(), false);
                  if (!quick.isSafe() && quick.getHazardDistance() <= 2) {
                     this.releaseForwardAttack();
                     this.currentWaypoint = null;
                     if (this.pathfinder != null && this.pathfinder.currentDetour != null) {
                        this.pathfinder.currentDetour.clear();
                        this.pathfinder.isDetouring = false;
                     }

                     this.currentState = MiningState.SEARCHING_SAFE_MINEDOWN;
                  }
               }
            } else if (dirToWaypoint == null) {
               this.currentWaypoint = null;
            }

         }
      }
   }

   private void initiateRTPFallback() {
      this.searchAttempts = 0;
      this.safeMineDownTarget = null;
      if (this.rtpWhenStuck.getValue()) {
         ++this.rtpAttempts;
         if (this.rtpAttempts < 5) {
            this.initiateRTP();
         } else {
            this.toggle(false);
         }
      } else {
         this.toggle(false);
      }

   }

   private void initiateRTP() {
      if (this.rtpWhenStuck.getValue()) {
         this.inRTPRecovery = true;
         long since = System.currentTimeMillis() - this.lastRtpTime;
         if (this.lastRtpTime > 0L && since < 16000L) {
            this.currentState = MiningState.RTP_COOLDOWN;
         } else {
            this.preRtpPos = this.mc.player.getBlockPos();
            RTPRegion region = (RTPRegion)this.rtpRegion.getValue();
            this.mc.player.networkHandler.sendChatMessage("/rtp " + region.cmd());
            this.currentState = MiningState.RTP_INITIATED;
            this.rtpCommandSent = true;
            ++this.rtpAttempts;
         }
      }
   }

   private boolean isInRTPState() {
      boolean var10000;
      switch (this.currentState) {
         case RTP_INITIATED:
         case RTP_WAITING:
         case RTP_COOLDOWN:
         case RTP_SCANNING_GROUND:
         case MINING_DOWN:
         case SEARCHING_SAFE_MINEDOWN:
         case MOVING_TO_MINEDOWN:
            var10000 = true;
            break;
         default:
            var10000 = this.inRTPRecovery;
      }

      return var10000;
   }

   private boolean isCenteredOnBlock(BlockPos blockPos) {
      double ox = Math.abs(this.mc.player.getX() - ((double)blockPos.getX() + (double)0.5F));
      double oz = Math.abs(this.mc.player.getZ() - ((double)blockPos.getZ() + (double)0.5F));
      return ox <= (double)0.25F && oz <= (double)0.25F;
   }

   private void handleExpRepairTick() {
      ++this.expThrowTicks;
      if (this.expThrowTicks >= this.repairDuration.getIntValue() * 20) {
         this.isThrowingExp = false;
         this.lastExpThrowTime = System.currentTimeMillis();
         this.currentState = this.stateBeforeExp;
         this.rotationController.startRotation(this.mc.player.getYaw(), 0.0F, () -> {
         });
      }

   }

   private boolean shouldRepairNow() {
      if (!this.autoRepair.getValue()) {
         return false;
      } else {
         ItemStack stack = this.mc.player.getMainHandStack();
         if (stack != null && !stack.isEmpty() && (stack.getItem() instanceof MiningToolItem || stack.getItem() instanceof ShearsItem)) {
            int max = stack.getMaxDamage();
            if (max <= 0) {
               return false;
            } else {
               double percent = (double)(max - stack.getDamage()) / (double)max * (double)100.0F;
               return percent <= this.repairThreshold.getValue();
            }
         } else {
            return false;
         }
      }
   }

   private void startRepairSequence() {
      this.isThrowingExp = true;
      this.expThrowTicks = 0;
      this.stateBeforeExp = this.currentState;
      this.currentState = MiningState.ROTATING;
      this.rotationController.setPreciseLanding(true);
      this.rotationController.startRotation(this.mc.player.getYaw(), 90.0F, () -> this.rotationController.setPreciseLanding(false));
   }

   private void handleIntervalMining() {
      this.setKey(this.mc.options.sneakKey, true);
      if (!this.intervalMiningActive) {
         this.intervalMiningActive = true;
         this.intervalStartTime = System.currentTimeMillis();
         this.intervalHolding = true;
         this.setKey(this.mc.options.attackKey, true);
      } else {
         long now = System.currentTimeMillis();
         long elapsed = now - this.intervalStartTime;
         if (this.intervalHolding) {
            if ((double)elapsed >= this.intervalHoldDuration.getValue()) {
               this.setKey(this.mc.options.attackKey, false);
               this.intervalHolding = false;
               this.intervalStartTime = now;
            }
         } else if ((double)elapsed >= this.intervalPauseDuration.getValue()) {
            this.setKey(this.mc.options.attackKey, true);
            this.intervalHolding = true;
            this.intervalStartTime = now;
         }

      }
   }

   private void releaseForwardAttack() {
      this.setKey(this.mc.options.attackKey, false);
      this.setKey(this.mc.options.forwardKey, false);
      this.setKey(this.mc.options.sneakKey, false);
   }

   private void startMiningForward() {
      this.setKey(this.mc.options.forwardKey, true);
      this.setKey(this.mc.options.attackKey, true);
   }

   private void stopMiningForward() {
      this.releaseForwardAttack();
   }

   private void setKey(KeyBinding key, boolean pressed) {
      key.setPressed(pressed);
   }

   private Direction yawToCardinal(float yaw) {
      float y = (yaw % 360.0F + 360.0F) % 360.0F;
      if (!(y >= 315.0F) && !(y < 45.0F)) {
         if (y >= 45.0F && y < 135.0F) {
            return Direction.WEST;
         } else {
            return y >= 135.0F && y < 225.0F ? Direction.NORTH : Direction.EAST;
         }
      } else {
         return Direction.SOUTH;
      }
   }

   private float directionToYaw(Direction dir) {
      float var10000;
      switch (dir) {
         case Direction.SOUTH -> var10000 = 180.0F;
         case Direction.NORTH -> var10000 = 0.0F;
         case Direction.EAST -> var10000 = -90.0F;
         case Direction.WEST -> var10000 = 90.0F;
         default -> var10000 = 0.0F;
      }

      return var10000;
   }

   private Direction getDirectionToward(BlockPos from, BlockPos to) {
      int dx = to.getX() - from.getX();
      int dz = to.getZ() - from.getZ();
      if (Math.abs(dx) > Math.abs(dz)) {
         return dx > 0 ? Direction.EAST : Direction.WEST;
      } else if (Math.abs(dz) > Math.abs(dx)) {
         return dz > 0 ? Direction.SOUTH : Direction.NORTH;
      } else if (dx != 0) {
         return dx > 0 ? Direction.EAST : Direction.WEST;
      } else if (dz != 0) {
         return dz > 0 ? Direction.SOUTH : Direction.NORTH;
      } else {
         return null;
      }
   }

   private boolean isTool(ItemStack stack) {
      return stack.getItem() instanceof MiningToolItem || stack.getItem() instanceof ShearsItem;
   }

   private void checkForPlayers() {
      if (this.mc.world != null) {
         for(AbstractClientPlayerEntity p : this.mc.world.getPlayers()) {
            if (p != this.mc.player && p.distanceTo(this.mc.player) <= 128.0F) {
               if (this.disconnectOnBaseFind.getValue()) {
                  this.mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("Player " + p.getName().getString() + " nearby")));
                  this.toggle(false);
               }
               break;
            }
         }

      }
   }

   private BlockPos findSafeMineDownSpot(BlockPos playerPos) {
      int radius = this.safeMineDownSearchRadius.getIntValue();

      for(int r = 2; r <= radius; ++r) {
         for(Object posObj : this.spiralRing(playerPos, r)) {
            BlockPos pos = (BlockPos)posObj;
            if (pos.getY() == playerPos.getY() && this.isPositionReachable(playerPos, pos) && this.canSafelyMineDownFrom(pos)) {
               return pos;
            }
         }
      }

      return null;
   }

   private List spiralRing(BlockPos center, int radius) {
      List<BlockPos> list = new ArrayList();

      for(int x = -radius; x <= radius; ++x) {
         list.add(center.add(x, 0, -radius));
         list.add(center.add(x, 0, radius));
      }

      for(int z = -radius + 1; z < radius; ++z) {
         list.add(center.add(-radius, 0, z));
         list.add(center.add(radius, 0, z));
      }

      return list;
   }

   private boolean isPositionReachable(BlockPos from, BlockPos to) {
      int dx = to.getX() - from.getX();
      int dz = to.getZ() - from.getZ();
      if (dx == 0 && dz == 0) {
         return true;
      } else {
         boolean xFirst = true;
         if (dx != 0) {
            Direction xDir = dx > 0 ? Direction.EAST : Direction.WEST;
            PathScanner.ScanResult xScan = this.pathScanner.scanDirection(from, xDir, Math.abs(dx), this.scanHeight.getIntValue(), false);
            if (!xScan.isSafe()) {
               xFirst = false;
            } else if (dz != 0) {
               BlockPos mid = from.offset(xDir, Math.abs(dx));
               Direction zDir = dz > 0 ? Direction.SOUTH : Direction.NORTH;
               PathScanner.ScanResult zScan = this.pathScanner.scanDirection(mid, zDir, Math.abs(dz), this.scanHeight.getIntValue(), false);
               if (!zScan.isSafe()) {
                  xFirst = false;
               }
            }
         } else if (dz != 0) {
            Direction zDir = dz > 0 ? Direction.SOUTH : Direction.NORTH;
            PathScanner.ScanResult zScan = this.pathScanner.scanDirection(from, zDir, Math.abs(dz), this.scanHeight.getIntValue(), false);
            xFirst = zScan.isSafe();
         }

         if (xFirst) {
            return true;
         } else if (dz != 0) {
            Direction zDir = dz > 0 ? Direction.SOUTH : Direction.NORTH;
            PathScanner.ScanResult zScan = this.pathScanner.scanDirection(from, zDir, Math.abs(dz), this.scanHeight.getIntValue(), false);
            if (!zScan.isSafe()) {
               return false;
            } else if (dx != 0) {
               BlockPos mid = from.offset(zDir, Math.abs(dz));
               Direction xDir = dx > 0 ? Direction.EAST : Direction.WEST;
               PathScanner.ScanResult xScan = this.pathScanner.scanDirection(mid, xDir, Math.abs(dx), this.scanHeight.getIntValue(), false);
               return xScan.isSafe();
            } else {
               return true;
            }
         } else {
            return false;
         }
      }
   }

   private boolean canSafelyMineDownFrom(BlockPos pos) {
      if (this.scanGroundBelowDetailed(pos) != BStarPhantom.GroundHazard.NONE) {
         return false;
      } else {
         BlockPos predictive = pos.down(5);
         if (this.scanGroundBelowDetailed(predictive) != BStarPhantom.GroundHazard.NONE) {
            return false;
         } else {
            BlockPos deep = pos.down(8);
            return this.scanGroundBelowDetailed(deep) == BStarPhantom.GroundHazard.NONE;
         }
      }
   }

   private List createPathToMineDown(BlockPos from, BlockPos to) {
      List<BlockPos> path = new ArrayList();
      int dx = to.getX() - from.getX();
      int dz = to.getZ() - from.getZ();
      if (dx != 0) {
         path.add(from.add(dx, 0, 0));
      }

      if (dz != 0) {
         path.add(to);
      }

      if (path.isEmpty() && !from.equals(to)) {
         path.add(to);
      }

      return path;
   }

   private GroundHazard scanGroundBelowDetailed(BlockPos start) {
      World world = this.mc.world;
      int radius = this.groundScanSize.getIntValue() / 2;
      boolean hasFluid = false;
      boolean hasVoid = false;
      boolean hasWeb = false;
      boolean hasVines = false;
      int voidCheckRadius = Math.min(radius, 1);

      for(int x = -voidCheckRadius; x <= voidCheckRadius; ++x) {
         for(int z = -voidCheckRadius; z <= voidCheckRadius; ++z) {
            int solidDepth = 0;
            boolean foundSolid = false;

            for(int d = 1; d <= 15; ++d) {
               BlockPos below = start.add(x, -d, z);
               BlockState st = world.getBlockState(below);
               if (!st.isAir() && st.isSolidBlock(world, below)) {
                  solidDepth = d;
                  foundSolid = true;
                  break;
               }
            }

            if (!foundSolid || solidDepth >= this.voidDepthThreshold.getIntValue()) {
               hasVoid = true;
            }
         }
      }

      for(int depth = 1; depth <= 10; ++depth) {
         for(int x = -radius; x <= radius; ++x) {
            for(int z = -radius; z <= radius; ++z) {
               BlockPos below = start.add(x, -depth, z);
               BlockState state = world.getBlockState(below);
               Block block = state.getBlock();
               if (state.getFluidState().getFluid() == Fluids.LAVA || state.getFluidState().getFluid() == Fluids.FLOWING_LAVA || state.getFluidState().getFluid() == Fluids.WATER || state.getFluidState().getFluid() == Fluids.FLOWING_WATER) {
                  hasFluid = true;
               }

               if (block == Blocks.COBWEB) {
                  hasWeb = true;
               }

               if (block == Blocks.VINE || block == Blocks.WEEPING_VINES || block == Blocks.WEEPING_VINES_PLANT || block == Blocks.TWISTING_VINES || block == Blocks.TWISTING_VINES_PLANT || block == Blocks.CAVE_VINES || block == Blocks.CAVE_VINES_PLANT) {
                  hasVines = true;
               }
            }
         }
      }

      if (!hasFluid && !hasWeb && !hasVines) {
         if (hasVoid) {
            return BStarPhantom.GroundHazard.VOID;
         } else {
            return BStarPhantom.GroundHazard.NONE;
         }
      } else if (hasVoid) {
         return BStarPhantom.GroundHazard.BOTH;
      } else {
         return BStarPhantom.GroundHazard.FLUIDS;
      }
   }

   private void releaseAllMovement() {
      this.setKey(this.mc.options.attackKey, false);
      this.setKey(this.mc.options.forwardKey, false);
      this.setKey(this.mc.options.backKey, false);
      this.setKey(this.mc.options.leftKey, false);
      this.setKey(this.mc.options.rightKey, false);
      this.setKey(this.mc.options.sneakKey, false);
   }

   private int searchChunkForSpawnersOptimized(Chunk chunk) {
      if (this.mc.player.getY() > (double)64.0F) {
         return 0;
      } else {
         int found = 0;
         BlockPos.Mutable mut = new BlockPos.Mutable();
         int playerY = (int)this.mc.player.getY();
         int minY = Math.max(this.mc.world.getBottomY(), -64);
         int maxY = Math.min(playerY + 20, 20);

         for(int x = chunk.getPos().getStartX(); x <= chunk.getPos().getEndX(); ++x) {
            for(int z = chunk.getPos().getStartZ(); z <= chunk.getPos().getEndZ(); ++z) {
               for(int y = minY; y <= maxY; ++y) {
                  mut.set(x, y, z);
                  if (chunk.getBlockState(mut).getBlock() == Blocks.SPAWNER) {
                     ++found;
                     return 1;
                  }
               }
            }
         }

         return found;
      }
   }

   static {
      DEFAULT_STORAGE = List.of(net.minecraft.block.entity.BlockEntityType.CHEST, net.minecraft.block.entity.BlockEntityType.BARREL, net.minecraft.block.entity.BlockEntityType.SHULKER_BOX, net.minecraft.block.entity.BlockEntityType.ENDER_CHEST, net.minecraft.block.entity.BlockEntityType.FURNACE, net.minecraft.block.entity.BlockEntityType.DISPENSER, net.minecraft.block.entity.BlockEntityType.DROPPER, net.minecraft.block.entity.BlockEntityType.HOPPER);
      RTP_COOLDOWN_PATTERN = Pattern.compile("You can't rtp for another (\\d+)s");
   }

   public static enum RTPRegion {
      EU_CENTRAL("eu central"),
      EU_WEST("eu west"),
      NA_EAST("east"),
      NA_WEST("west"),
      OCEANIA("oceania"),
      ASIA("asia");

      private final String command;

      private RTPRegion(String cmd) {
         this.command = cmd;
      }

      public String cmd() {
         return this.command;
      }

      // $FF: synthetic method
      private static RTPRegion[] $values() {
         return new RTPRegion[]{EU_CENTRAL, EU_WEST, NA_EAST, NA_WEST, OCEANIA, ASIA};
      }
   }

   private static enum GroundHazard {
      NONE,
      FLUIDS,
      VOID,
      BOTH;

      // $FF: synthetic method
      private static GroundHazard[] $values() {
         return new GroundHazard[]{NONE, FLUIDS, VOID, BOTH};
      }
   }
}
