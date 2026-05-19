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
    cursor.execute('''CREATE TABLE IF NOT EXISTS accounts 
                      (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT UNIQUE, password TEXT, username TEXT)''')
    cursor.execute('''CREATE TABLE IF NOT EXISTS users 
                      (id INTEGER PRIMARY KEY, username TEXT, level INTEGER, experience INTEGER)''')
    cursor.execute('''CREATE TABLE IF NOT EXISTS completed_quests
                      (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, quest_id TEXT, 
                       FOREIGN KEY(user_id) REFERENCES users(id))''')
    conn.commit()
    conn.close()

init_db()

@strawberry.type
class UserProfile:
    username: str
    level: int
    experience: int
    xp_for_next_level: int

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

@strawberry.type
class CompleteQuestResponse:
    username: str
    level: int
    experience: int
    xp_gained: int
    leveled_up: bool
    xp_for_next_level: int

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

# --- (QUERIES) ---
@strawberry.type
class Query:
    @strawberry.field
    def me(self, token: str) -> UserProfile:
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()
        cursor.execute("SELECT username, level, experience FROM users WHERE id = ?", (token,))
        row = cursor.fetchone()
        conn.close()
        if row:
            level = row[1]
            total_xp = row[2]
            threshold = xp_for_level(level)
            return UserProfile(username=row[0], level=level, experience=total_xp, xp_for_next_level=threshold)
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
                (email, password, username)
            )
            account_id = cursor.lastrowid
            cursor.execute(
                "INSERT INTO users (id, username, level, experience) VALUES (?, ?, ?, ?)",
                (account_id, username, 1, 0)
            )
            conn.commit()
            conn.close()
            return AuthResponse(success=True, message="Cont creat cu succes!", token=str(account_id))
        except Exception as e:
            conn.close()
            return AuthResponse(success=False, message=f"Eroare: {str(e)}")

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

# --- starting the server ---
schema = strawberry.Schema(query=Query, mutation=Mutation)
graphql_app = GraphQLRouter(schema)

app = FastAPI()
app.include_router(graphql_app, prefix="/graphql")