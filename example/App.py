from google.oauth2 import service_account
from google.auth.transport.requests import Request
import requests
import json

SERVICE_ACCOUNT_FILE = "serviceAccount.json"
PROJECT_ID = "livedigital-sdk-example"

credentials = service_account.Credentials.from_service_account_file(
    SERVICE_ACCOUNT_FILE,
    scopes=["https://www.googleapis.com/auth/firebase.messaging"]
)
credentials.refresh(Request())
access_token = credentials.token

headers = {
    "Authorization": f"Bearer {access_token}",
    "Content-Type": "application/json; UTF-8"
}

fcm_token = "put token from example here"
payload = {
    "message": {
        "token": fcm_token,
        "data": {
            "caller": "Room 777",
            "callerNumber": "+79999999999",
            "roomAlias": "qNF8SCvEu7"
        },
        "android": {
            "priority": "HIGH",
            "ttl": "20s"
        }
    }
}

response = requests.post(
    f"https://fcm.googleapis.com/v1/projects/{PROJECT_ID}/messages:send",
    headers=headers,
    data=json.dumps(payload)
)

print(response.status_code)
print(response.text)