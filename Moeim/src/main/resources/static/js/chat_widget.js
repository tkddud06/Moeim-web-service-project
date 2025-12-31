// chat_widget.js

// 전역 상태
let currentRoomId = null;
let currentRoomTitle = null;
let chatPollTimer = null;

function openChatWidgetWithRoom(roomId, title) {
    currentRoomId = roomId;
    currentRoomTitle = title || '채팅';

    const panel = document.getElementById('chat-widget-panel');
    const headerTitle = document.getElementById('chat-widget-room-title');
    const messagesEl = document.getElementById('chat-widget-messages');

    if (headerTitle) {
        headerTitle.textContent = currentRoomTitle;
    }
    if (messagesEl) {
        messagesEl.innerHTML = '';
    }

    // 위젯 열기 (애니메이션 class는 이미 만들어 둔 걸 사용)
    if (panel) {
        panel.classList.add('open');   // CSS에서 .open 에 scale/opacity 애니메이션 걸려있다고 가정
    }

    // 기존 폴링 있으면 정리
    if (chatPollTimer) {
        clearInterval(chatPollTimer);
    }

    // 메시지 로드 + 주기 폴링
    loadWidgetMessages();
    chatPollTimer = setInterval(loadWidgetMessages, 5000);
}

// 1:1 채팅 열기(마이페이지 버튼에서 호출)
window.openDirectChat = async function (targetUserId, targetNickname) {
    try {
        const res = await fetch(`/api/chat/direct/${targetUserId}`, {
            method: 'POST'
        });

        if (!res.ok) {
            alert('채팅방을 여는 데 실패했습니다.');
            return;
        }

        const room = await res.json(); // { id, roomKey, name }
        const title = room.name || targetNickname || '1:1 채팅';

        openChatWidgetWithRoom(room.id, title);
    } catch (e) {
        console.error(e);
        alert('채팅방을 여는 중 오류가 발생했습니다.');
    }
};


// ========== 위젯 내부 메시지 로딩 / 전송 ==========

async function loadWidgetMessages() {
    if (!currentRoomId) return;

    const messagesEl = document.getElementById('chat-widget-messages');
    if (!messagesEl) return;

    try {
        const res = await fetch(`/api/chat/messages?roomId=${currentRoomId}`);
        if (!res.ok) return;

        const data = await res.json(); // List<ChatMessageDTO>

        messagesEl.innerHTML = '';
        data.forEach(msg => {
            const row = document.createElement('div');
            // loginUserId는 layout.html에서 data-login-user-id 같은 걸로 넘겨서 사용
            const loginUserId = parseInt(
                document.body.getAttribute('data-login-user-id') || '0'
            );
            const isMe = (msg.senderId === loginUserId);

            row.className = 'chat-msg-row ' + (isMe ? 'me' : 'other');

            const bubble = document.createElement('div');
            bubble.className = 'chat-msg-bubble';
            bubble.textContent = msg.content || '';

            row.appendChild(bubble);
            messagesEl.appendChild(row);
        });

        messagesEl.scrollTop = messagesEl.scrollHeight;
    } catch (e) {
        console.error('위젯 메시지 로드 오류', e);
    }
}

async function sendWidgetMessage() {
    const inputEl = document.getElementById('chat-widget-input');
    if (!inputEl || !currentRoomId) return;

    const text = inputEl.value.trim();
    if (!text) return;

    try {
        const res = await fetch('/api/chat/send', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                roomId: currentRoomId,
                content: text
            })
        });

        if (!res.ok) {
            alert('메시지 전송에 실패했습니다.');
            return;
        }

        inputEl.value = '';
        await loadWidgetMessages();
    } catch (e) {
        console.error('위젯 메시지 전송 오류', e);
        alert('메시지 전송 중 오류가 발생했습니다.');
    }
}


// ========== 위젯 토글(이미 버튼/애니메이션 있다면 id만 맞추면 됨) ==========

document.addEventListener('DOMContentLoaded', function () {
    const toggleBtn = document.getElementById('chat-widget-toggle');
    const panel = document.getElementById('chat-widget-panel');
    const inputEl = document.getElementById('chat-widget-input');
    const sendBtn = document.getElementById('chat-widget-send');

    if (toggleBtn && panel) {
        toggleBtn.addEventListener('click', function () {
            panel.classList.toggle('open');

            // 처음 열릴 때 현재 방 있으면 메시지 로딩
            if (panel.classList.contains('open') && currentRoomId) {
                loadWidgetMessages();
            }
        });
    }

    if (sendBtn) {
        sendBtn.addEventListener('click', sendWidgetMessage);
    }

    if (inputEl) {
        inputEl.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendWidgetMessage();
            }
        });
    }
});
