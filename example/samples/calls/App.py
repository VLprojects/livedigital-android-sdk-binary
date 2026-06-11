import os
import sys
import tkinter as tk
from tkinter import messagebox
from google.oauth2 import service_account
from google.auth.transport.requests import Request
import requests
import json

SERVICE_ACCOUNT_FILE = "serviceAccount.json"
PROJECT_ID = "livedigital-sdk-example"

def get_resource_path(relative_path):
    try:
        base_path = sys._MEIPASS
    except AttributeError:
        base_path = os.path.abspath(".")
    return os.path.join(base_path, relative_path)

key_path = get_resource_path('serviceAccount.json')

def get_access_token():
    credentials = service_account.Credentials.from_service_account_file(
        key_path,
        scopes=["https://www.googleapis.com/auth/firebase.messaging"]
    )
    credentials.refresh(Request())
    return credentials.token

def send_push(push_type):
    """Универсальная функция для отправки всех типов пушей"""
    fcm_token = token_entry.get().strip()
    caller = caller_entry.get().strip()
    number = number_entry.get().strip()
    room_alias = room_entry.get().strip()
    call_type = call_type_var.get().lower()

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
                    "type": push_type,
                    "caller": caller,
                    "callerNumber": number,
                    "roomAlias": room_alias,
                    "callType": call_type # Добавляем тип в data
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
            messagebox.showinfo("Успех", f"Push '{push_type}' ({call_type}) отправлен")
        else:
            messagebox.showerror("Ошибка FCM", f"Status: {response.status_code}\n{response.text}")

    except Exception as e:
        messagebox.showerror("Ошибка", str(e))

def send_call_start_push(): send_push("call_start")
def send_call_end_push(): send_push("call_end")
def send_call_answered_push(): send_push("call_answered")

root = tk.Tk()
root.title("FCM Push Sender")
root.geometry("500x600")

frame = tk.Frame(root, padx=10, pady=10)
frame.pack(fill="both", expand=True)
frame.columnconfigure(1, weight=1)

tk.Label(frame, text="FCM Token").grid(row=0, column=0, sticky="w", pady=5)
token_entry = tk.Entry(frame)
token_entry.grid(row=0, column=1, sticky="ew", pady=5)

tk.Label(frame, text="Caller").grid(row=1, column=0, sticky="w", pady=5)
caller_entry = tk.Entry(frame)
caller_entry.grid(row=1, column=1, sticky="ew", pady=5)

tk.Label(frame, text="Caller Number").grid(row=2, column=0, sticky="w", pady=5)
number_entry = tk.Entry(frame)
number_entry.grid(row=2, column=1, sticky="ew", pady=5)

tk.Label(frame, text="Room Alias").grid(row=3, column=0, sticky="w", pady=5)
room_entry = tk.Entry(frame)
room_entry.grid(row=3, column=1, sticky="ew", pady=5)


tk.Label(frame, text="Call Type").grid(row=4, column=0, sticky="w", pady=5)
call_type_var = tk.StringVar(root)
call_type_var.set("Video")
call_type_dropdown = tk.OptionMenu(frame, call_type_var, "Video", "Audio")
call_type_dropdown.grid(row=4, column=1, sticky="ew", pady=5)

tk.Button(frame, text="Send Call Start Push", command=send_call_start_push).grid(row=5, column=0, columnspan=2, pady=10, sticky="ew")
tk.Button(frame, text="Send Call End Push", command=send_call_end_push).grid(row=6, column=0, columnspan=2, pady=10, sticky="ew")
tk.Button(frame, text="Send Call Answered Push", command=send_call_answered_push).grid(row=7, column=0, columnspan=2, pady=10, sticky="ew")

root.mainloop()