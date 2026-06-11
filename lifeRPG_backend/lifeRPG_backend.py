import re

import strawberry
from fastapi import FastAPI
from strawberry.fastapi import GraphQLRouter
import sqlite3
from typing import Optional
import random
import math

def init_db():
    conn = sqlite3.connect("lifequest.db")
    cursor = conn.cursor()
    # Tabela pentru conturi
    cursor.execute('''CREATE TABLE IF NOT EXISTS accounts 
                      (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT UNIQUE, password TEXT, username TEXT)''')
    # Tabela pentru profilul de joc
    cursor.execute('''CREATE TABLE IF NOT EXISTS users
                  (id INTEGER PRIMARY KEY, username TEXT, level INTEGER, experience INTEGER, avatar TEXT DEFAULT 'default_avatar')''')
    try:
        cursor.execute(
            "ALTER TABLE users ADD COLUMN avatar TEXT DEFAULT 'default_avatar'"
        )
    except:
        pass
    cursor.execute('''CREATE TABLE IF NOT EXISTS storylines
                      (id INTEGER PRIMARY KEY AUTOINCREMENT,
                   title TEXT,
                   description TEXT)''')
    cursor.execute('''CREATE TABLE IF NOT EXISTS quests
                      (id INTEGER PRIMARY KEY AUTOINCREMENT,
                   title TEXT,
                   description TEXT,
                   xp_reward INTEGER,
                   category TEXT,
                   difficulty TEXT,
                   storyline_id INTEGER NULL,
                   storyline_step INTEGER NULL,
                   FOREIGN KEY(storyline_id) REFERENCES storylines(id))''')
    cursor.execute('''CREATE TABLE IF NOT EXISTS user_storylines
                     (id INTEGER PRIMARY KEY AUTOINCREMENT,
                   user_id INTEGER,
                   storyline_id INTEGER,
                   current_step INTEGER DEFAULT 1,
                   FOREIGN KEY(user_id) REFERENCES users(id),
                   FOREIGN KEY(storyline_id) REFERENCES storylines(id),
                   UNIQUE(user_id, storyline_id)) ''')
    conn.commit()

    cursor.execute("SELECT COUNT(*) FROM storylines")
    if cursor.fetchone()[0] == 0:
        print("Inserting default storylines...")

        cursor.execute("INSERT INTO storylines (title, description) VALUES (?, ?)",
                       ("Get in Shape!", "Embark on a fitness journey to improve your health and well-being. Complete quests that challenge you to walk, jog, do sit-ups, and push-ups."))
        story1_id = cursor.lastrowid

        quests_story1 = [
            ("Warm up", "Walk 500 meters to get your body moving.", 100, "walking", "Easy", 1),
            ("Wake your abs up!", "Complete 20 sit-ups.", 200, "situps", "Hard", 2),
            ("Push Through!", "Complete 15 push-ups.", 150, "pushups", "Medium", 3),
            ("Hard Mode!", "Complete 50 sit-ups.", 500, "situps", "Hard", 4),
            ("Full Body Challenge!", "Do a 30 minutes cardio session!", 600, "other", "Hard", 5)
        ]
        for q in quests_story1:
            cursor.execute("""INSERT INTO quests (title, description, xp_reward, category, difficulty, storyline_id, storyline_step)
                           VALUES (?, ?, ?, ?, ?, ?, ?) """, (q[0], q[1], q[2], q[3], q[4], story1_id, q[5]))

        cursor.execute("INSERT INTO storylines (title, description) VALUES (?, ?)",
                       ("The Writer's Journey", "Draw inspiration from the world around you and become a master storyteller. Complete quests that encourage you to read, write, and explore your creativity."))
        
        story2_id = cursor.lastrowid
        quests_story2 = [
            ("Inspiration Farming", "Read a book of your choice (min. 155 pages).", 100, "reading", "Easy", 1),
            ("Caracter Creation", "Write a detailed character profile for a story idea you have.", 50, "other", "Medium", 2),
            ("Learning from the bests", "Read a classic novel (e.g., 'To Kill a Mockingbird', '1984', 'Pride and Prejudice').", 100, "reading", "Medium", 3),
            ("Plotting the Journey", "Outline the plot of a story you want to write, including main events and character arcs.", 150, "other", "Medium", 4),
            ("The MasterPiece", "Write a short story (at least 1000 words) and share it with friends or online.", 300, "other", "Hard", 5)
        ]
        for q in quests_story2:
            cursor.execute("""INSERT INTO quests (title, description, xp_reward, category, difficulty, storyline_id, storyline_step)
                           VALUES (?, ?, ?, ?, ?, ?, ?) """, (q[0], q[1], q[2], q[3], q[4], story2_id, q[5]))
        conn.commit()
        print("Default storylines and quests inserted.")

    conn.close()

def calculate_level(total_xp: int) -> int:
    level = 1
    xp_required = 100
    accumulated = 0
    while accumulated + xp_required <= total_xp:
        accumulated += xp_required
        level += 1
        xp_required = int(xp_required * 1.2)
    return level

def xp_for_level(level: int) -> int:
    xp_required = 100
    for _ in range(level - 1):
        xp_required = int(xp_required * 1.2)
    return xp_required

def generate_quest(quest_type: str) -> dict:
    if quest_type == "walking":
        distance = random.choice([250, 500, 750, 1000, 1500, 2000])
        xp = int(50 * (distance / 250))
        return {
            "title": f"Walk {distance} meters",
            "description": f"Take a walk and cover {distance} meters.",
            "xp_reward": xp,
            "category": "fitness",
            "difficulty": "Easy" if distance <= 500 else "Medium" if distance <= 1000 else "Hard",
            "quest_type": "walking",
            "target": distance
        }
    elif quest_type == "jogging":
        distance = random.choice([500, 750, 1000, 1500, 2000])
        xp = int(75 * (distance / 500))
        return {
            "title": f"Jog {distance} meters",
            "description": f"Pick up the pace and jog {distance} meters.",
            "xp_reward": xp,
            "category": "fitness",
            "difficulty": "Easy" if distance <= 750 else "Medium" if distance <= 1250 else "Hard",
            "quest_type": "jogging",
            "target": distance
        }
    elif quest_type == "situps":
        reps = random.choice([3, 5, 10, 15, 20, 25, 30])
        xp = int(30 * (reps / 3))
        return {
            "title": f"Do {reps} sit-ups",
            "description": f"Complete {reps} sit-ups.",
            "xp_reward": xp,
            "category": "fitness",
            "difficulty": "Easy" if reps <= 5 else "Medium" if reps <= 15 else "Hard",
            "quest_type": "situps",
            "target": reps
        }
    elif quest_type == "pushups":
        reps = random.choice([3, 5, 10, 15, 20, 25, 30])
        xp = int(30 * (reps / 3))
        return {
            "title": f"Do {reps} push-ups",
            "description": f"Complete {reps} push-ups.",
            "xp_reward": xp,
            "category": "fitness",
            "difficulty": "Easy" if reps <= 5 else "Medium" if reps <= 15 else "Hard",
            "quest_type": "pushups",
            "target": reps
        }
    elif quest_type == "reading":
        return {
            "title": "Read a book",
            "description": "Pick up a book and read it.",
            "xp_reward": 100,
            "category": "education",
            "difficulty": "Hard",
            "quest_type": "reading",
            "target": 1
        }
    elif quest_type == "photo":
        subjects = ["a tree", "a flower", "a building", "a sunset", "a bird", "a river"]
        subject = random.choice(subjects)
        return {
            "title": f"Find {subject}",
            "description": f"Go outside and take a picture of {subject}.",
            "xp_reward": 40,
            "category": "exploration",
            "difficulty": "Easy",
            "quest_type": "photo",
            "target": 1
        }

def calculate_forged_xp(quest_type: str, target: int) -> int:
    if quest_type == "walking":
        return int(50 * (target / 250))
    elif quest_type == "jogging":
        return int(75 * (target / 500))
    elif quest_type == "situps":
        return int(30 * (target / 3))
    elif quest_type == "pushups":
        return int(30 * (target / 3))
    elif quest_type == "reading":
        return 100
    elif quest_type == "photo":
        return 40
    elif quest_type == "other":
        return 50
    return 50

def generate_forge_description(quest_type: str, target_str: str) -> str:
    if quest_type == "walking":
        return f"Walk {target_str} meters."
    elif quest_type == "jogging":
        return f"Jog {target_str} meters."
    elif quest_type == "situps":
        return f"Complete {target_str} sit-ups."
    elif quest_type == "pushups":
        return f"Complete {target_str} push-ups."
    elif quest_type == "reading":
        return "Read a book of your choice."
    elif quest_type == "photo":
        return f"Take a photo of {target_str}."
    elif quest_type == "other":
        return "Complete your custom quest."
    return "Complete the custom quest."

def is_valid_password(password: str) -> tuple[bool, str]:
    if len(password) < 3:
        return False, "Password must contain at least 3 characters."

    if any(c in password for c in ['\0', '\n', '\r', '\t']):
        return False, "Password contains invalid characters."

    return True, ""

init_db()

@strawberry.type
class UserProfile:
    username: str
    level: int
    experience: int
    xp_for_next_level: int
    avatar: str

@strawberry.type
class AuthResponse:
    success: bool
    message: str
    token: str | None = None

@strawberry.type
class Quest:
    id: str
    title: str
    description: str
    xp_reward: int
    category: str
    difficulty: str
    quest_type: str
    target: int
    storyline_id: int | None = None
    storyline_step: int | None = None

@strawberry.type
class Storyline:
    id: int
    title: str
    description: str

@strawberry.type
class CompleteQuestResponse:
    username: str
    level: int
    experience: int
    xp_gained: int
    leveled_up: bool
    xp_for_next_level: int



# --- (QUERIES) ---
@strawberry.type
class Query:
    @strawberry.field
    def me(self, token: str) -> UserProfile:
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()
        cursor.execute("SELECT username, level, experience, avatar FROM users WHERE id = ?", (token,))
        row = cursor.fetchone()
        conn.close()
        if row:
            level = row[1]
            total_xp = row[2]
            threshold = xp_for_level(level)
            return UserProfile(username=row[0], level=level, experience=total_xp, xp_for_next_level=threshold, avatar=row[3])
        return None

    @strawberry.field
    def get_random_quests(self, count: int = 7) -> list[Quest]:
        quest_types = ["walking", "jogging", "situps", "pushups", "reading", "photo"]
        selected_types = random.choices(quest_types, k=count)
        quests = []
        for i, qt in enumerate(selected_types):
            q = generate_quest(qt)
            q["id"] = f"{qt}_{i}_{random.randint(1000, 9999)}"
            quests.append(Quest(**q))
        return quests
    
    @strawberry.field
    def get_storylines(self) -> list[Storyline]:
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()
        cursor.execute("SELECT id, title, description FROM storylines")
        rows = cursor.fetchall()
        conn.close()
        return [Storyline(id=row[0], title=row[1], description=row[2]) for row in rows]
    
    @strawberry.field
    def get_active_storyline_quest(self, user_id: int) -> Quest | None:
        import sqlite3
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()
        cursor.execute(""" 
            SELECT storyline_id, current_step
                       FROM user_storylines
                       WHERE user_id = ?
                       LIMIT 3""", (user_id,))
        rows = cursor.fetchall()
        if not rows:
            conn.close()
            return None
        
        titles = []
        descriptions = []
        total_xp = 0
        main_q_id = ""
        main_category = "other"
        main_difficulty = "Easy"
        

        for storyline_id, current_step in rows:
            cursor.execute("""
                       SELECT id, title, description, xp_reward, category, difficulty
                       FROM quests
                       WHERE storyline_id = ? AND storyline_step = ?""", (storyline_id, current_step))
            step = cursor.fetchone()

            if step:
                q_id, title, description, xp_reward, actual_category, difficulty = step
                titles.append(title)
                descriptions.append(f"*{description}")
                total_xp += xp_reward
                main_q_id += f"{q_id}_"
                main_category = actual_category
                main_difficulty = difficulty
        conn.close()

        if not titles:
            return None
        
        combined_title = " / ".join(titles)
        combined_description = "\n".join(descriptions)

        numbers = re.findall(r'\d+', combined_description)
        target_value = int(numbers[0]) if numbers else 1
        

        return Quest(
            id=f"storyline_{main_q_id}", # string unic
            title=f"Active adventures: {combined_title}",
            description=combined_description,
            xp_reward=total_xp,
            category="storyline",  # lasam storyline ca sa stie fronted ca vrem mov 
            difficulty=main_difficulty,
            quest_type=main_category,
            target=target_value,
            storyline_id=rows[0][0],  # luam storyline_id din primul quest activ (daca sunt mai multe, e ok sa fie acelasi)
            storyline_step=rows[0][1]  # luam current_step din primul quest activ (daca sunt mai multe, e ok sa fie acelasi)
        )

    @strawberry.field
    def get_replacement_quest(self, exclude_types: list[str]) -> Optional[Quest]:
        quest_types = ["walking", "jogging", "situps", "pushups", "reading", "photo"]
        q = generate_quest(random.choice(quest_types))
        q["id"] = f"{q['quest_type']}_{random.randint(1000, 9999)}"
        return Quest(**q)

    @strawberry.field
    def get_completed_quests(self, user_id: int) -> list[Quest]:
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()
        cursor.execute("""
            SELECT quest_id, title, description, xp_reward, category, difficulty, quest_type, target
            FROM completed_quests_log
            WHERE user_id = ?
            ORDER BY id DESC
        """, (user_id,))
        rows = cursor.fetchall()
        conn.close()
        return [Quest(id=row[0], title=row[1], description=row[2], xp_reward=row[3],
                      category=row[4], difficulty=row[5], quest_type=row[6], target=row[7])
                for row in rows]

    @strawberry.field
    def get_completed_quests_count(self, user_id: int) -> int:
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM completed_quests_log WHERE user_id = ?", (user_id,))
        count = cursor.fetchone()[0]
        conn.close()
        return count

# --- (MUTATIONS) ---
@strawberry.type
class Mutation:
    @strawberry.mutation
    def register(self, email: str, password: str, username: str) -> AuthResponse:
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()

        # Email validation
        if not email or not email.strip():
            conn.close()
            return AuthResponse(
                success=False,
                message="Email cannot be blank."
            )

        # Username validation
        if not username or not username.strip():
            conn.close()
            return AuthResponse(
                success=False,
                message="Username cannot be blank."
            )

        # Password validation
        if len(password) < 3:
            conn.close()
            return AuthResponse(
                success=False,
                message="Password must contain at least 3 characters."
            )

        if any(c in password for c in ['\0', '\n', '\r', '\t']):
            conn.close()
            return AuthResponse(
                success=False,
                message="Password contains invalid characters."
            )

        cursor.execute("SELECT id FROM accounts WHERE email = ?", (email,))
        if cursor.fetchone():
            conn.close()
            return AuthResponse(success=False, message="email already exists.")

        cursor.execute("SELECT id FROM accounts WHERE username = ?", (username,))
        if cursor.fetchone():
            conn.close()
            return AuthResponse(success=False, message="username already exists.")

        try:
            cursor.execute(
                "INSERT INTO accounts (email, password, username) VALUES (?, ?, ?)",
                (email.strip(), password, username.strip())
            )

            account_id = cursor.lastrowid

            cursor.execute(
                "INSERT INTO users (id, username, level, experience) VALUES (?, ?, ?, ?)",
                (account_id, username.strip(), 1, 0)
            )

            conn.commit()
            conn.close()

            return AuthResponse(
                success=True,
                message="Cont creat cu succes!",
                token=str(account_id)
            )

        except Exception as e:
            conn.close()
            return AuthResponse(
                success=False,
                message=f"Eroare: {str(e)}"
            )

    @strawberry.mutation
    def login(self, email: str, password: str) -> AuthResponse:
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()
        cursor.execute("SELECT id, password FROM accounts WHERE email = ?", (email,))
        row = cursor.fetchone()
        conn.close()
        if not row:
            return AuthResponse(success=False, message="No account exists for this address.")
        if row[1] != password:
            return AuthResponse(success=False, message="Incorrect password.")
        return AuthResponse(success=True, message="Login reusit!", token=str(row[0]))

    @strawberry.mutation
    def complete_quest(self, user_id: int, quest_id: str, title: str, description: str,
                       xp_reward: int, category: str, difficulty: str,
                       quest_type: str, target: int) -> CompleteQuestResponse:
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()
        cursor.execute("SELECT username, level, experience FROM users WHERE id = ?", (user_id,))
        user = cursor.fetchone()
        if not user:
            conn.close()
            raise Exception("User not found.")
        username, current_level, current_xp = user
        new_total_xp = current_xp + xp_reward
        new_level = calculate_level(new_total_xp)
        leveled_up = new_level > current_level
        cursor.execute(
            "UPDATE users SET level = ?, experience = ? WHERE id = ?",
            (new_level, new_total_xp, user_id)
        )
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS completed_quests_log
            (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, quest_id TEXT,
             title TEXT, description TEXT, xp_reward INTEGER, category TEXT,
             difficulty TEXT, quest_type TEXT, target INTEGER)
        """)
        cursor.execute("""
            INSERT INTO completed_quests_log 
            (user_id, quest_id, title, description, xp_reward, category, difficulty, quest_type, target)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (user_id, quest_id, title, description, xp_reward, category, difficulty, quest_type, target))
        conn.commit()
        conn.close()
        return CompleteQuestResponse(
            username=username,
            level=new_level,
            experience=new_total_xp,
            xp_gained=xp_reward,
            leveled_up=leveled_up,
            xp_for_next_level=xp_for_level(new_level)
        )
    @strawberry.mutation
    def forge_quest(self, user_id: int, title: str, quest_type: str, target: int, target_label: str = "") -> Quest:
        xp_reward = calculate_forged_xp(quest_type, target)
        quest_id = f"forged_{user_id}_{quest_type}_{random.randint(1000, 9999)}"
        description = generate_forge_description(quest_type, target_label if target_label else str(target))
        difficulty = "Easy" if xp_reward <= 50 else "Medium" if xp_reward <= 100 else "Hard"
        return Quest(
            id=quest_id,
            title=title,
            description=description,
            xp_reward=xp_reward,
            category="forged",
            difficulty=difficulty,
            quest_type=quest_type,
            target=target
        )
    
    @strawberry.mutation
    def start_storyline(self, user_id: int, storyline_id:int) -> bool:
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()
        try:
            cursor.execute("""INSERT OR IGNORE INTO user_storylines (user_id, storyline_id, current_step) VALUES (?, ?, 1)""", (user_id, storyline_id))
            conn.commit()
            return True
        except Exception as e:
            print(f"Error starting storyline: {e}")
            return False
        finally:
            conn.close()
            
    @strawberry.mutation
    def update_avatar(self, user_id: int, avatar: str) -> bool:
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()

        try:
            cursor.execute(
                "UPDATE users SET avatar = ? WHERE id = ?",
                (avatar, user_id)
            )

            conn.commit()
            return True

        except Exception as e:
            print(f"Avatar update error: {e}")
            return False

        finally:
            conn.close()

    

# --- starting the server ---
schema = strawberry.Schema(query=Query, mutation=Mutation)
graphql_app = GraphQLRouter(schema)

app = FastAPI()
app.include_router(graphql_app, prefix="/graphql")