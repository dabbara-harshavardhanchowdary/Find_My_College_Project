import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";
import { getAuth, GoogleAuthProvider } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js";

const firebaseConfig = {
  apiKey: "AIzaSyCwDNh_IOkoUqotv0U7XokihdFcE1ncVig",
  authDomain: "find-my-college-222.firebaseapp.com",
  projectId: "find-my-college-222",
  storageBucket: "find-my-college-222.firebasestorage.app",
  messagingSenderId: "1002371358622",
  appId: "1:1002371358622:web:79058985180421c33dc08f",
  measurementId: "G-8Y19RMT5N8"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();