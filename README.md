# ShapeShifters

<p align="center">
  <a href="https://www.youtube.com/watch?v=SfAvzy_RjkA">
    <img src="https://img.youtube.com/vi/SfAvzy_RjkA/0.jpg" alt="Watch the Demo" width="640">
  </a>
</p>

**ShapeShifters** is a fast-paced multiplayer 3D maze game where players take on the roles of hunter and hunted in a dynamic, ever-changing environment filled with traps, treasures, and NPCs. Built with **Java3D**, the game combines real-time networking, interactive animations, and immersive design to deliver a thrilling local or LAN experience.

---

## 🎮 Game Features

- 🧭 **3D Maze Exploration** – Navigate a procedurally generated maze with moving walls, traps, and hidden treasures.
- 👻 **Shape-Shifting Gameplay** – Morph into NPCs to hide or gain abilities. Players can take on different roles like predator or prey.
- 💡 **Dynamic Environment** – Includes shooting star background, textured walls, lighting effects, and animated structures.
- 🧠 **Collision & Interaction** – Real-time detection between players, NPCs, and the environment. Prey collects treasure; predator eliminates.
- 🕹️ **Multiplayer Support** – Connect over a network to compete in real-time with up to two players.
- 🔊 **Audio + Visual Polish** – Includes sound effects, endgame animations, a cinematic camera, and styled menus.

---

## 🧱 Built With

- **Java3D**
- **Swing** (for menus and UI)
- **Java Socket Networking**
- **Blender** (for modeling assets)
- **Agile (Scrum)** methodology for project planning

---

## 🚀 How to Play

1. **Launch `GameMenu.java`**
2. **Enter IP and Username**
3. **Play as either:**
   - 🟥 **Predator** – click to eliminate prey
   - 🟦 **Prey** – morph into NPCs, evade predator, collect treasure

Use `WASD` to move and `E` to interact with the treasure when nearby.

---

## 🧩 Core Classes

| Class Name               | Responsibility                                              |
|--------------------------|-------------------------------------------------------------|
| `BasicScene.java`        | Main game logic, rendering, networking, and camera control |
| `GameMenu.java`          | Launch menu with styled UI and IP input                    |
| `CollisionDetector.java` | AABB-based collision detection between all game objects     |
| `NPC.java`               | Logic and behavior for NPC movement and appearance         |
| `TreasureKeyBehavior.java` | Handles treasure collection interaction and morphing     |
| `GhostModel.java`        | Loads and manages ghost model appearance and behavior       |

---

## 🧪 Testing & Improvements

- ✅ **Manual unit and integration testing** after each feature and merge
- 👥 **User testing feedback** led to smoother movement and improved game balance
- 🔁 **Game balanced** through adjusted NPC collision, morph timers, and predator kill range

---

## 🧑‍💻 Team

- Avnoor Ludhar  
- Fied Elahresh  
- Lucas Omstead  
- Aleksa Vucak  
- Tegveer Bhatti  

---

## 📦 Status

ShapeShifters is a finished course project and not actively maintained, but feel free to fork or contribute!
