package dev.gambleclient.module.modules.ai;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class DirectionalPathfinder {
   private Direction primaryDirection;
   private Direction originalPrimaryDirection;
   public Queue currentDetour = new LinkedList();
   public boolean isDetouring = false;
   private final PathScanner pathScanner;
   private static final int APPROACH_STOP_DISTANCE = 7;
   private static final int DETOUR_START_BUFFER = 5;
   private static final int DETOUR_SIDE_CLEARANCE = 2;
   private int detourCount = 0;
   private int directionChangeCount = 0;
   private String lastDecisionReason = "";

   public DirectionalPathfinder(PathScanner scanner) {
      this.pathScanner = scanner;
   }

   public void setInitialDirection(Direction dir) {
      this.primaryDirection = dir;
      this.originalPrimaryDirection = dir;
   }

   public Direction getPrimaryDirection() {
      return this.primaryDirection;
   }

   public boolean isDetouring() {
      return this.isDetouring;
   }

   public Queue peekAllWaypoints() {
      return new LinkedList(this.currentDetour);
   }

   public BlockPos getNextWaypoint() {
      if (this.currentDetour.isEmpty()) {
         this.isDetouring = false;
         return null;
      } else {
         return (BlockPos)this.currentDetour.poll();
      }
   }

   public BlockPos peekNextWaypoint() {
      return (BlockPos)this.currentDetour.peek();
   }

   public PathPlan calculateDetour(BlockPos playerPos, PathScanner.ScanResult hazard) {
      if (hazard.getHazardDistance() > 10) {
         return this.createApproachPlan(playerPos, hazard);
      } else {
         HazardBounds bounds = this.scanHazardBoundaries(playerPos, hazard);
         List<BlockPos> leftPath = this.buildMinimalDetourPath(playerPos, bounds, true);
         List<BlockPos> rightPath = this.buildMinimalDetourPath(playerPos, bounds, false);
         if (leftPath != null && rightPath != null) {
            return leftPath.size() <= rightPath.size() ? this.createDetourPlan(leftPath, "Left detour") : this.createDetourPlan(rightPath, "Right detour");
         } else if (leftPath != null) {
            return this.createDetourPlan(leftPath, "Left detour");
         } else {
            return rightPath != null ? this.createDetourPlan(rightPath, "Right detour") : this.handleCompletelyBlocked(playerPos);
         }
      }
   }

   private PathPlan createApproachPlan(BlockPos playerPos, PathScanner.ScanResult hazard) {
      int safeApproachDistance = Math.max(hazard.getHazardDistance() - 7, 0);
      if (safeApproachDistance <= 0) {
         return new PathPlan(false, new LinkedList(), "Already close to hazard");
      } else {
         BlockPos approachPoint = playerPos.offset(this.primaryDirection, safeApproachDistance);
         if (!this.validateSegment(playerPos, approachPoint, this.primaryDirection, safeApproachDistance)) {
            return this.handleCompletelyBlocked(playerPos);
         } else {
            List<BlockPos> approachPath = new ArrayList();
            approachPath.add(approachPoint);
            Queue<BlockPos> waypoints = new LinkedList(approachPath);
            this.currentDetour = new LinkedList(waypoints);
            this.isDetouring = true;
            return new PathPlan(true, waypoints, "Approaching distant hazard");
         }
      }
   }

   private List buildMinimalDetourPath(BlockPos playerPos, HazardBounds bounds, boolean goLeft) {
      Direction sideDir = goLeft ? this.primaryDirection.rotateYCounterclockwise() : this.primaryDirection.rotateYClockwise();
      int sideDistance = bounds.width / 2 + 2;

      for(int attempt = 0; attempt < 3; ++attempt) {
         List<BlockPos> path = this.tryDetourPath(playerPos, bounds, sideDir, sideDistance + attempt);
         if (path != null) {
            return path;
         }
      }

      return null;
   }

   private List tryDetourPath(BlockPos playerPos, HazardBounds bounds, Direction sideDir, int sideDistance) {
      List<BlockPos> waypoints = new ArrayList();
      int approachDistance = Math.max(bounds.startDistance - 5, 1);
      int forwardPastHazard = bounds.depth + 3;
      BlockPos turnPoint = this.adjustToGroundLevel(playerPos.offset(this.primaryDirection, approachDistance));
      if (!this.validateSegment(playerPos, turnPoint, this.primaryDirection, approachDistance)) {
         return null;
      } else {
         waypoints.add(turnPoint);
         BlockPos sidePoint = this.adjustToGroundLevel(turnPoint.offset(sideDir, sideDistance));
         if (!this.validateSegment(turnPoint, sidePoint, sideDir, sideDistance)) {
            return null;
         } else {
            waypoints.add(sidePoint);
            BlockPos pastHazard = this.adjustToGroundLevel(sidePoint.offset(this.primaryDirection, forwardPastHazard));
            if (!this.validateSegment(sidePoint, pastHazard, this.primaryDirection, forwardPastHazard)) {
               return null;
            } else {
               waypoints.add(pastHazard);
               return waypoints;
            }
         }
      }
   }

   private BlockPos adjustToGroundLevel(BlockPos pos) {
      PathScanner.ScanResult currentScan = this.pathScanner.scanDirection(pos, this.primaryDirection, 0, 1, false);
      if (currentScan.getHazardType() == PathScanner.HazardType.UNSAFE_GROUND || !this.isGroundSolid(pos)) {
         BlockPos oneDown = pos.down();
         if (this.isGroundSolid(oneDown)) {
            return oneDown;
         }

         BlockPos twoDown = pos.down(2);
         if (this.isGroundSolid(twoDown)) {
            return twoDown;
         }
      }

      BlockPos oneUp = pos.up();
      PathScanner.ScanResult upScan = this.pathScanner.scanDirection(oneUp, this.primaryDirection, 0, 1, false);
      return upScan.isSafe() && !this.isGroundSolid(pos) && this.isGroundSolid(pos.down()) ? oneUp : pos;
   }

   private boolean isGroundSolid(BlockPos pos) {
      PathScanner.ScanResult groundCheck = this.pathScanner.scanDirection(pos.up(), this.primaryDirection, 0, 1, false);
      return groundCheck.isSafe() || groundCheck.getHazardType() != PathScanner.HazardType.LAVA && groundCheck.getHazardType() != PathScanner.HazardType.WATER && groundCheck.getHazardType() != PathScanner.HazardType.UNSAFE_GROUND;
   }

   private boolean validateSegment(BlockPos start, BlockPos end, Direction moveDir, int distance) {
      if (distance <= 10) {
         for(int i = 1; i <= distance; ++i) {
            BlockPos checkPos = start.offset(moveDir, i);
            PathScanner.ScanResult scan = this.pathScanner.scanDirection(checkPos, moveDir, 1, 4, false);
            if (!scan.isSafe()) {
               return false;
            }

            if (moveDir != this.primaryDirection && i == distance) {
               PathScanner.ScanResult forwardCheck = this.pathScanner.scanDirection(checkPos, this.primaryDirection, 2, 4, false);
               if (!forwardCheck.isSafe() && forwardCheck.getHazardDistance() <= 1) {
                  return false;
               }
            }

            if (!this.checkGroundSafety(checkPos)) {
               return false;
            }
         }

         return true;
      } else {
         BlockPos currentPos = start;

         for(int chunk = 0; chunk < distance; chunk += 5) {
            int chunkSize = Math.min(5, distance - chunk);

            for(int i = 1; i <= chunkSize; ++i) {
               BlockPos checkPos = currentPos.offset(moveDir, i);
               int scanAhead = distance > 10 ? 2 : 1;
               PathScanner.ScanResult scan = this.pathScanner.scanDirection(checkPos, moveDir, scanAhead, 4, false);
               if (!scan.isSafe()) {
                  return false;
               }

               if (!this.checkGroundSafety(checkPos)) {
                  return false;
               }
            }

            currentPos = currentPos.offset(moveDir, chunkSize);
         }

         return true;
      }
   }

   private boolean checkGroundSafety(BlockPos pos) {
      for(int depth = 1; depth <= 2; ++depth) {
         BlockPos below = pos.down(depth);

         for(Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            PathScanner.ScanResult scan = this.pathScanner.scanDirection(below, dir, 0, 1, false);
            if (!scan.isSafe() && scan.getHazardDistance() == 0 && scan.getHazardType() == PathScanner.HazardType.LAVA) {
               return false;
            }
         }
      }

      return true;
   }

   private PathPlan createDetourPlan(List waypoints, String reason) {
      Queue<BlockPos> waypointQueue = new LinkedList(waypoints);
      this.currentDetour = new LinkedList(waypointQueue);
      this.isDetouring = true;
      ++this.detourCount;
      this.lastDecisionReason = "Detour #" + this.detourCount + ": " + reason;
      return new PathPlan(true, waypointQueue, this.lastDecisionReason);
   }

   private HazardBounds scanHazardBoundaries(BlockPos playerPos, PathScanner.ScanResult initialHazard) {
      int hazardDistance = initialHazard.getHazardDistance();
      BlockPos hazardCenter = playerPos.offset(this.primaryDirection, hazardDistance);
      int leftWidth = 0;
      int rightWidth = 0;
      int forwardDepth = 0;
      Direction leftDir = this.primaryDirection.rotateYCounterclockwise();
      Direction rightDir = this.primaryDirection.rotateYClockwise();

      for(int i = 1; i <= 10; leftWidth = i++) {
         BlockPos checkPos = hazardCenter.offset(leftDir, i);
         PathScanner.ScanResult scan = this.pathScanner.scanDirection(checkPos, this.primaryDirection, 1, 4, false);
         if (scan.isSafe()) {
            break;
         }
      }

      for(int i = 1; i <= 10; rightWidth = i++) {
         BlockPos checkPos = hazardCenter.offset(rightDir, i);
         PathScanner.ScanResult scan = this.pathScanner.scanDirection(checkPos, this.primaryDirection, 1, 4, false);
         if (scan.isSafe()) {
            break;
         }
      }

      for(int i = 0; i <= 20; forwardDepth = i++) {
         BlockPos checkPos = hazardCenter.offset(this.primaryDirection, i);
         PathScanner.ScanResult scan = this.pathScanner.scanDirection(checkPos, this.primaryDirection, 1, 4, false);
         if (scan.isSafe()) {
            break;
         }
      }

      int totalWidth = leftWidth + rightWidth + 1;
      int totalDepth = Math.max(forwardDepth, 1);
      return new HazardBounds(hazardDistance, totalWidth, totalDepth, hazardCenter);
   }

   private PathPlan handleCompletelyBlocked(BlockPos playerPos) {
      Direction[] alternatives = new Direction[]{this.primaryDirection.rotateYClockwise(), this.primaryDirection.rotateYCounterclockwise()};

      for(Direction newDir : alternatives) {
         PathScanner.ScanResult scan = this.pathScanner.scanDirection(playerPos, newDir, 20, 4, false);
         if (scan.isSafe()) {
            ++this.directionChangeCount;
            this.lastDecisionReason = "Perpendicular direction change #" + this.directionChangeCount;
            return new PathPlan(newDir, this.lastDecisionReason);
         }
      }

      return new PathPlan((Direction)null, "No valid paths - RTP recovery needed");
   }

   public void completeDetour() {
      this.isDetouring = false;
      this.currentDetour.clear();
   }

   public String getDebugInfo() {
      return this.primaryDirection == null ? "Primary: NOT SET | Pathfinder not initialized" : String.format("Primary: %s (original: %s) | Detouring: %s | Detours: %d | Changes: %d | Reason: %s", this.primaryDirection.getName(), this.originalPrimaryDirection != null ? this.originalPrimaryDirection.getName() : "NOT SET", this.isDetouring, this.detourCount, this.directionChangeCount, this.lastDecisionReason);
   }

   public static class PathPlan {
      public final boolean needsDetour;
      public final Queue waypoints;
      public final String reason;
      public final Direction newPrimaryDirection;

      public PathPlan(boolean needsDetour, Queue waypoints, String reason) {
         this.needsDetour = needsDetour;
         this.waypoints = waypoints;
         this.reason = reason;
         this.newPrimaryDirection = null;
      }

      public PathPlan(Direction newDirection, String reason) {
         this.needsDetour = false;
         this.waypoints = new LinkedList();
         this.reason = reason;
         this.newPrimaryDirection = newDirection;
      }
   }

   private static class HazardBounds {
      final int startDistance;
      final int width;
      final int depth;
      final BlockPos center;

      HazardBounds(int startDistance, int width, int depth, BlockPos center) {
         this.startDistance = startDistance;
         this.width = width;
         this.depth = depth;
         this.center = center;
      }
   }
}
