import strawberry
from fastapi import FastAPI
from strawberry.fastapi import GraphQLRouter
import sqlite3
from typing import Optional

def init_db():
    conn = sqlite3.connect("lifequest.db")
    cursor = conn.cursor()
    # Tabela pentru conturi
    cursor.execute('''CREATE TABLE IF NOT EXISTS accounts 
                      (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT UNIQUE, password TEXT, username TEXT)''')
    # Tabela pentru profilul de joc
    cursor.execute('''CREATE TABLE IF NOT EXISTS users 
                      (id INTEGER PRIMARY KEY, username TEXT, level INTEGER, experience INTEGER)''')

    # Tabela pentru quest-uri
    cursor.execute('''CREATE TABLE IF NOT EXISTS quests
                      (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, description TEXT, xp_reward INTEGER, category TEXT, difficulty TEXT, is_completed INTEGER DEFAULT 0)''')

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
    id: int
    title: str
    description: str
    xp_reward: int
    category: str
    difficulty: str
    is_completed: bool

# --- (QUERIES) ---
@strawberry.type
class Query:
    @strawberry.field
    def me(self, token: str) -> UserProfile:
        # the token is the users ID
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()
        cursor.execute("SELECT username, level, experience FROM users WHERE id = ?", (token,))
        row = cursor.fetchone()
        conn.close()
        
        if row:
            level = row[1]
            total_xp = row[2]
            threshold = xp_for_level(level)
            return UserProfile(username=row[0], level=level,  experience=total_xp, xp_for_next_level=threshold)
        return None
    
    @strawberry.field
    def get_quests(self) -> list[Quest]:
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()
        cursor.execute("SELECT id, title, description, xp_reward, category, difficulty, is_completed FROM quests")
        rows = cursor.fetchall()
        conn.close()
        
        return [Quest(id=row[0], title=row[1], description=row[2], xp_reward=row[3], category=row[4], difficulty=row[5], is_completed=bool(row[6])) for row in rows]

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
    def complete_quest(self, user_id: int, xp_reward: int) -> CompleteQuestResponse:
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