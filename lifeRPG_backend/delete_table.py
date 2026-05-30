import sqlite3

try:
    conn = sqlite3.connect('lifequest.db')
    cursor = conn.cursor()

    cursor.execute('DROP TABLE IF EXISTS quests')
    print("Table 'quests' deleted successfully.")

    cursor.execute('DROP TABLE IF EXISTS completed_quests')
    print("Table 'completed_quests' deleted successfully.")

    target_user_id = 5

    cursor.execute('DELETE FROM users WHERE id = ?', (target_user_id,))
    cursor.execute('DELETE FROM accounts WHERE id = ?', (target_user_id,))
    print(f"User with ID {target_user_id} and associated account deleted successfully.")

    conn.commit()

except Exception as e:
    print(f"An error occurred: {e}")
finally:
    if conn:
        conn.close()