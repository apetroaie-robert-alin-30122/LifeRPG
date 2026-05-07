import strawberry
from fastapi import FastAPI
from strawberry.fastapi import GraphQLRouter
import sqlite3

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

@strawberry.type
class AuthResponse:
    success: bool
    message: str
    token: str = None

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
            return UserProfile(username=row[0], level=row[1], experience=row[2])
        return None
    
    @strawberry.field
    def get_quests(self) -> list[Quest]:
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()
        cursor.execute("SELECT id, title, description, xp_reward, category, difficulty, is_completed FROM quests")
        rows = cursor.fetchall()
        conn.close()
        
        return [Quest(id=row[0], title=row[1], description=row[2], xp_reward=row[3], category=row[4], difficulty=row[5], is_completed=bool(row[6])) for row in rows]




# --- (MUTATIONS) ---
@strawberry.type
class Mutation:
    @strawberry.mutation
    def register(self, email: str, password: str, username: str) -> AuthResponse:
        try:
            conn = sqlite3.connect("lifequest.db")
            cursor = conn.cursor()
            # Save in account
            cursor.execute("INSERT INTO accounts (email, password, username) VALUES (?, ?, ?)", (email, password, username))
            account_id = cursor.lastrowid
            # Save in user
            cursor.execute("INSERT INTO users (id, username, level, experience) VALUES (?, ?, ?, ?)", 
                           (account_id, username, 1, 0))
            conn.commit()
            conn.close()
            return AuthResponse(success=True, message="Cont creat cu succes!", token=str(account_id))
        except Exception as e:
            return AuthResponse(success=False, message=f"Eroare: {str(e)}")

    @strawberry.mutation
    def login(self, email: str, password: str) -> AuthResponse:
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM accounts WHERE email = ? AND password = ?", (email, password))
        row = cursor.fetchone()
        conn.close()
        
        if row:
            return AuthResponse(success=True, message="Login reusit!", token=str(row[0]))
        return AuthResponse(success=False, message="Email sau parola incorecta!")

    @strawberry.mutation
    def complete_quest(self, user_id: int, quest_id: int) -> UserProfile:
        conn = sqlite3.connect("lifequest.db")
        cursor = conn.cursor()

    # Verificăm dacă quest-ul există și nu a fost deja completat
        cursor.execute("SELECT difficulty, is_completed FROM quests WHERE id = ?", (quest_id,))
        quest = cursor.fetchone()

        if not quest or quest[1] == 1:
            conn.close()
            raise Exception("Quest-ul nu există sau a fost deja terminat!")
        
        # XP reward bazat pe dificultate
        diff = quest[0]
        if diff == "Easy":
            xp_reward = 50
        elif diff == "Medium":
            xp_reward = 100
        elif diff == "Hard":
            xp_reward = 200
        else:
            xp_reward = 50
        
        # Cerem datele curente ale utilizatorului
        cursor.execute("SELECT username, level, experience FROM users WHERE id = ?", (user_id,))
        user = cursor.fetchone()
        username, current_level, current_xp = user

        # Calculam logica de level-up
        # Formula: 1000 * ( 1.25 ^ (level - 1) )
        new_xp = current_xp + xp_reward
        new_level = current_level

        # Verificăm dacă utilizatorul a crescut în nivel
        xp_needed = int(1000 * (1.25 ** (new_level - 1)))

        while new_xp >= xp_needed:
            new_xp -= xp_needed
            new_level += 1
            # Recalculăm XP-ul necesar pentru următorul nivel
            xp_needed = int(1000 * (1.25 ** (new_level - 1)))

        # Salvăm noile date ale utilizatorului
        cursor.execute("UPDATE users SET level = ?, experience = ? WHERE id = ?", (new_level, new_xp, user_id))
        cursor.execute("UPDATE quests SET is_completed = 1 WHERE id = ?", (quest_id,))

        conn.commit()
        conn.close()

        return UserProfile(username=username, level=new_level, experience=new_xp)


# --- starting the server ---
schema = strawberry.Schema(query=Query, mutation=Mutation)
graphql_app = GraphQLRouter(schema)

app = FastAPI()
app.include_router(graphql_app, prefix="/graphql")