import tkinter as tk
from tkinter import messagebox
from google.oauth2 import service_account
from google.auth.transport.requests import Request
import requests
import json

SERVICE_ACCOUNT_FILE = "serviceAccount.json"
PROJECT_ID = "livedigital-sdk-example"

def get_access_token():
    credentials = service_account.Credentials.from_service_account_file(
        SERVICE_ACCOUNT_FILE,
        scopes=["https://www.googleapis.com/auth/firebase.messaging"]
    )
    credentials.refresh(Request())
    return credentials.token

def send_call_start_push():
    fcm_token = token_entry.get().strip()
    caller = caller_entry.get().strip()
    number = number_entry.get().strip()
    room_alias = room_entry.get().strip()

    if not fcm_token or not caller or not number or not room_alias:
        messagebox.showerror("Ошибка", "Все поля должны быть заполнены")
        return

    try:
        access_token = get_access_token()

        headers = {
            "Authorization": f"Bearer {access_token}",
            "Content-Type": "application/json; UTF-8"
        }

        payload = {
            "message": {
                "token": fcm_token,
                "data": {
                    "type": "call_start",
                    "caller": caller,
                    "callerNumber": number,
                    "roomAlias": room_alias
                },
                "android": {
                    "priority": "HIGH",
                    "ttl": "3600s"
                }
            }
        }

        response = requests.post(
            f"https://fcm.googleapis.com/v1/projects/{PROJECT_ID}/messages:send",
            headers=headers,
            data=json.dumps(payload)
        )

        if response.status_code == 200:
            messagebox.showinfo("Успех", "Push отправлен успешно")
        else:
            messagebox.showerror(
                "Ошибка FCM",
                f"Status: {response.status_code}\n{response.text}"
            )

    except Exception as e:
        messagebox.showerror("Ошибка", str(e))

def send_call_end_push():
    fcm_token = token_entry.get().strip()
    caller = caller_entry.get().strip()
    number = number_entry.get().strip()
    room_alias = room_entry.get().strip()

    if not fcm_token or not caller or not number or not room_alias:
        messagebox.showerror("Ошибка", "Все поля должны быть заполнены")
        return

    try:
        access_token = get_access_token()

        headers = {
            "Authorization": f"Bearer {access_token}",
            "Content-Type": "application/json; UTF-8"
        }

        payload = {
            "message": {
                "token": fcm_token,
                "data": {
                    "type": "call_end",
                    "caller": caller,
                    "callerNumber": number,
                    "roomAlias": room_alias
                },
                "android": {
                    "priority": "HIGH",
                    "ttl": "3600s"
                }
            }
        }

        response = requests.post(
            f"https://fcm.googleapis.com/v1/projects/{PROJECT_ID}/messages:send",
            headers=headers,
            data=json.dumps(payload)
        )

        if response.status_code == 200:
            messagebox.showinfo("Успех", "Push отправлен успешно")
        else:
            messagebox.showerror(
                "Ошибка FCM",
                f"Status: {response.status_code}\n{response.text}"
            )

    except Exception as e:
        messagebox.showerror("Ошибка", str(e))

# GUI
root = tk.Tk()
root.title("FCM Push Sender")
root.geometry("500x300")

root.columnconfigure(0, weight=1)
root.rowconfigure(0, weight=1)

frame = tk.Frame(root, padx=10, pady=10)
frame.grid(sticky="nsew")

frame.columnconfigure(1, weight=1)

# Row 0
tk.Label(frame, text="FCM Token").grid(row=0, column=0, sticky="w", pady=5)
token_entry = tk.Entry(frame)
token_entry.grid(row=0, column=1, sticky="ew", pady=5)

# Row 1
tk.Label(frame, text="Caller").grid(row=1, column=0, sticky="w", pady=5)
caller_entry = tk.Entry(frame)
caller_entry.grid(row=1, column=1, sticky="ew", pady=5)

# Row 2
tk.Label(frame, text="Caller Number").grid(row=2, column=0, sticky="w", pady=5)
number_entry = tk.Entry(frame)
number_entry.grid(row=2, column=1, sticky="ew", pady=5)

# Row 3
tk.Label(frame, text="Room Alias").grid(row=3, column=0, sticky="w", pady=5)
room_entry = tk.Entry(frame)
room_entry.grid(row=3, column=1, sticky="ew", pady=5)

# Button
send_call_start_button = tk.Button(frame, text="Send Call Start Push", command=send_call_start_push)
send_call_start_button.grid(row=4, column=0, columnspan=2, pady=15, sticky="ew")

# Button
send_call_end_button = tk.Button(frame, text="Send Call End Push", command=send_call_end_push)
send_call_end_button.grid(row=5, column=0, columnspan=2, pady=15, sticky="ew")

root.mainloop()