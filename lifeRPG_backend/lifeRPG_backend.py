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

# --- starting the server ---
schema = strawberry.Schema(query=Query, mutation=Mutation)
graphql_app = GraphQLRouter(schema)

app = FastAPI()
app.include_router(graphql_app, prefix="/graphql")