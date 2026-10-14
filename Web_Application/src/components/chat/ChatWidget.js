/**
 * ChatWidget Component for Web Application
 * Renders floating FAB button, animated AI counselor chat drawer, typing indicator, and suggestion chips.
 */

import { sendChatMessageToAI } from "../../services/aiChatService.js";

let chatHistory = [];
let isWaiting = false;

export function initChatWidget() {
    const fabBtn = document.getElementById("aiChatFab");
    const drawer = document.getElementById("aiChatDrawer");
    const closeBtn = document.getElementById("closeAiChatBtn");
    const chatForm = document.getElementById("aiChatForm");
    const chatInput = document.getElementById("aiChatInput");
    const messagesContainer = document.getElementById("aiChatMessages");
    const typingIndicator = document.getElementById("aiTypingIndicator");
    const chipContainer = document.getElementById("aiSuggestionChips");

    if (!fabBtn || !drawer) return;

    // Toggle Chat Drawer Open/Close
    const toggleDrawer = () => {
        const isHidden = drawer.classList.contains("hidden");
        if (isHidden) {
            drawer.classList.remove("hidden");
            drawer.style.display = "flex";
            if (chatInput) chatInput.focus();
        } else {
            drawer.classList.add("hidden");
            drawer.style.display = "none";
        }
    };

    fabBtn.onclick = toggleDrawer;
    if (closeBtn) closeBtn.onclick = toggleDrawer;

    // Suggestion Chips Handler
    if (chipContainer) {
        chipContainer.querySelectorAll(".ai-chip").forEach(chip => {
            chip.onclick = () => {
                const promptText = chip.getAttribute("data-prompt") || chip.innerText.trim();
                if (chatInput) {
                    chatInput.value = promptText;
                    submitMessage(promptText);
                }
            };
        });
    }

    // Form Submit Handler
    if (chatForm) {
        chatForm.onsubmit = (e) => {
            e.preventDefault();
            const text = chatInput ? chatInput.value.trim() : "";
            if (text) submitMessage(text);
        };
    }

    async function submitMessage(text) {
        if (isWaiting || !text) return;
        if (chatInput) chatInput.value = "";

        // Render User Message
        appendMessage("user", text);
        chatHistory.push({ role: "user", content: text });

        // Show Typing Indicator
        isWaiting = true;
        if (typingIndicator) typingIndicator.classList.remove("hidden");
        scrollToBottom();

        try {
            const aiResponse = await sendChatMessageToAI(text, chatHistory);
            appendMessage("model", aiResponse);
            chatHistory.push({ role: "model", content: aiResponse });
        } catch (err) {
            appendMessage("model", "I'm sorry, I encountered an issue fetching AI insights. Please try again!");
        } finally {
            isWaiting = false;
            if (typingIndicator) typingIndicator.classList.add("hidden");
            scrollToBottom();
        }
    }

    function appendMessage(role, content) {
        if (!messagesContainer) return;

        const msgDiv = document.createElement("div");
        msgDiv.className = `flex ${role === "user" ? "justify-end" : "justify-start"} mb-3`;

        const bubble = document.createElement("div");
        bubble.className = `max-w-[85%] rounded-2xl p-3.5 text-xs leading-relaxed shadow-md ${
            role === "user" 
                ? "bg-blue-600 text-white rounded-br-none" 
                : "bg-slate-800 text-slate-100 border border-slate-700 rounded-bl-none space-y-1.5"
        }`;
        bubble.innerHTML = content;

        msgDiv.appendChild(bubble);
        messagesContainer.appendChild(msgDiv);
        scrollToBottom();
    }

    function scrollToBottom() {
        if (messagesContainer) {
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        }
    }

    // Initial Welcome Message
    if (messagesContainer && messagesContainer.children.length === 0) {
        appendMessage("model", "👋 Welcome! I am your <strong>FindMyCollege AI Counselor</strong>.<br/>Ask me about NIRF ranks, admission cutoffs, tuition fees, placement packages, or ROI calculations!");
    }
}
