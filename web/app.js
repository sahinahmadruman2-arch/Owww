/**
 * Karllo AI - Application Coordinator & Voice HUD Controller
 */

import { KarlloVoiceModule } from './speech-module.js';
import { JarvisVisualizer } from './audio-visualizer.js';
import { KarlloCoreEngine } from './karllo-core.js';

document.addEventListener('DOMContentLoaded', () => {
  const engine = new KarlloCoreEngine();

  // DOM Elements
  const canvas = document.getElementById('jarvisCanvas');
  const statusBadge = document.getElementById('statusBadge');
  const liveTranscriptBox = document.getElementById('liveTranscriptBox');
  const micBtn = document.getElementById('micBtn');
  const stopSpeakingBtn = document.getElementById('stopSpeakingBtn');
  const dialogueStream = document.getElementById('dialogueStream');
  const langSelect = document.getElementById('langSelect');
  const continuousToggleBtn = document.getElementById('continuousToggleBtn');
  const muteToggleBtn = document.getElementById('muteToggleBtn');
  const muteIcon = document.getElementById('muteIcon');
  const clearChatBtn = document.getElementById('clearChatBtn');
  const timerList = document.getElementById('timerList');
  const activeTimerBadge = document.getElementById('activeTimerBadge');
  const notesList = document.getElementById('notesList');
  const addNoteManualBtn = document.getElementById('addNoteManualBtn');

  // Top Left 3 Dots Menu & Top Left Speak Button
  const topMenuDotsBtn = document.getElementById('topMenuDotsBtn');
  const topDropdownMenu = document.getElementById('topDropdownMenu');
  const menuSpeakItem = document.getElementById('menuSpeakItem');
  const menuHandsFreeItem = document.getElementById('menuHandsFreeItem');
  const menuStopSpeakingItem = document.getElementById('menuStopSpeakingItem');
  const menuMuteItem = document.getElementById('menuMuteItem');
  const menuSettingsItem = document.getElementById('menuSettingsItem');
  const menuClearItem = document.getElementById('menuClearItem');
  const topLeftSpeakBtn = document.getElementById('topLeftSpeakBtn');
  const topLeftSpeakIcon = document.getElementById('topLeftSpeakIcon');
  const topLeftSpeakLabel = document.getElementById('topLeftSpeakLabel');

  // Conversation Direct Speaking & Input Elements
  const headerSpeakBtn = document.getElementById('headerSpeakBtn');
  const headerSpeakIcon = document.getElementById('headerSpeakIcon');
  const headerSpeakText = document.getElementById('headerSpeakText');
  const chatSpeakBtn = document.getElementById('chatSpeakBtn');
  const chatSpeakIcon = document.getElementById('chatSpeakIcon');
  const chatSpeakText = document.getElementById('chatSpeakText');
  const listeningIndicatorBar = document.getElementById('listeningIndicatorBar');
  const listeningIndicatorText = document.getElementById('listeningIndicatorText');
  const conversationTextInput = document.getElementById('conversationTextInput');
  const sendMessageBtn = document.getElementById('sendMessageBtn');

  // Modals
  const settingsModal = document.getElementById('settingsModal');
  const settingsModalBtn = document.getElementById('settingsModalBtn');
  const closeSettingsBtn = document.getElementById('closeSettingsBtn');
  const voiceSelect = document.getElementById('voiceSelect');
  const speechRate = document.getElementById('speechRate');
  const rateValue = document.getElementById('rateValue');
  const speechPitch = document.getElementById('speechPitch');
  const pitchValue = document.getElementById('pitchValue');
  const speechVolume = document.getElementById('speechVolume');
  const volumeValue = document.getElementById('volumeValue');
  const testVoiceBtn = document.getElementById('testVoiceBtn');

  const noteModal = document.getElementById('noteModal');
  const closeNoteBtn = document.getElementById('closeNoteBtn');
  const manualNoteText = document.getElementById('manualNoteText');
  const saveManualNoteBtn = document.getElementById('saveManualNoteBtn');

  // Timers tracker
  const activeTimers = [];

  // Initialize Voice Module
  const voice = new KarlloVoiceModule({
    lang: langSelect.value,
    rate: parseFloat(speechRate.value),
    pitch: parseFloat(speechPitch.value),
    volume: parseFloat(speechVolume.value),
    onStateChange: (state) => {
      updateHUDState(state);
    },
    onTranscript: (transcript, isFinal) => {
      liveTranscriptBox.textContent = transcript;
      conversationTextInput.value = transcript;
      if (isFinal) {
        liveTranscriptBox.classList.remove('interim');
        conversationTextInput.value = '';
        handleFinalSpokenInput(transcript);
      } else {
        liveTranscriptBox.classList.add('interim');
      }
    },
    onVoicesReady: (voices) => {
      populateVoiceList(voices);
    },
    onError: (err) => {
      liveTranscriptBox.textContent = err;
      liveTranscriptBox.classList.remove('interim');
      listeningIndicatorBar.classList.remove('active');
      setTimeout(() => {
        if (voice.state === 'IDLE') {
          liveTranscriptBox.textContent = 'Press the microphone button or say "Hey Karllo" to speak...';
        }
      }, 4000);
    }
  });

  // Initialize Canvas Visualizer
  const visualizer = new JarvisVisualizer(canvas, voice);
  visualizer.start();

  // Populate voice selector dropdown
  function populateVoiceList(voices) {
    voiceSelect.innerHTML = '<option value="">Default System Voice</option>';
    const currentLang = langSelect.value;

    voices.forEach((v) => {
      const opt = document.createElement('option');
      opt.value = v.name;
      opt.textContent = `${v.name} (${v.lang})`;
      if (v.lang.startsWith(currentLang.slice(0, 2))) {
        opt.textContent += ' ★';
      }
      voiceSelect.appendChild(opt);
    });
  }

  // Update Visual HUD state
  function updateHUDState(state) {
    statusBadge.className = 'status-badge ' + state.toLowerCase();
    statusBadge.textContent = state;

    if (state === 'LISTENING') {
      micBtn.classList.add('listening');
      micBtn.innerHTML = '🛑';
      micBtn.title = 'Stop Listening';
      liveTranscriptBox.textContent = 'Listening to your voice...';

      // Top Left Speak Button
      topLeftSpeakBtn.classList.add('listening');
      topLeftSpeakIcon.textContent = '🛑';
      topLeftSpeakLabel.textContent = 'Stop';

      // Update Conversation Elements
      headerSpeakBtn.classList.add('listening');
      headerSpeakIcon.textContent = '🛑';
      headerSpeakText.textContent = 'Stop';

      chatSpeakBtn.classList.add('listening');
      chatSpeakIcon.textContent = '🛑';
      chatSpeakText.textContent = 'Listening... (Tap to finish)';

      listeningIndicatorBar.classList.add('active');
      listeningIndicatorText.textContent = 'Listening to your voice... Speak now';
      conversationTextInput.placeholder = 'Speaking... words will appear here...';
    } else {
      micBtn.classList.remove('listening');
      micBtn.innerHTML = '🎙️';
      micBtn.title = 'Tap to Speak';

      // Top Left Speak Button
      topLeftSpeakBtn.classList.remove('listening');
      topLeftSpeakIcon.textContent = '🎙️';
      topLeftSpeakLabel.textContent = 'Speak';

      // Update Conversation Elements
      headerSpeakBtn.classList.remove('listening');
      headerSpeakIcon.textContent = '🎙️';
      headerSpeakText.textContent = 'Speak';

      chatSpeakBtn.classList.remove('listening');
      chatSpeakIcon.textContent = '🎙️';
      chatSpeakText.textContent = 'Speak';

      listeningIndicatorBar.classList.remove('active');
      conversationTextInput.placeholder = "Click 'Speak' or type your message here...";
    }

    if (state === 'SPEAKING') {
      stopSpeakingBtn.classList.add('visible');
    } else {
      stopSpeakingBtn.classList.remove('visible');
    }
  }

  // Handle User Input Submission
  async function handleFinalSpokenInput(input) {
    if (!input || !input.trim()) return;

    appendMessage('user', input);
    updateHUDState('PROCESSING');
    liveTranscriptBox.textContent = 'Processing neural reasoning...';

    // Query semantic engine
    const response = await engine.processQuery(input);

    appendMessage('karllo', response.text, response.intent);
    liveTranscriptBox.textContent = response.text;

    // Handle any dispatched actions
    if (response.action) {
      handleEngineAction(response.action);
    }

    // Speak response using TTS
    await voice.speak(response.text);
  }

  // Handle Engine Actions (Timers, Notes, etc.)
  function handleEngineAction(action) {
    if (action.type === 'START_TIMER') {
      createTimer(action.seconds, action.label);
    } else if (action.type === 'NOTE_SAVED') {
      renderNotesList();
    } else if (action.type === 'REMINDER_SET') {
      // Create temporary visual feedback
      createTimer(60, `Reminder: ${action.reminder.text}`);
    }
  }

  // Timer Management
  function createTimer(seconds, label) {
    const timerObj = {
      id: Date.now(),
      label: label || 'Timer',
      remaining: seconds,
      interval: null
    };

    timerObj.interval = setInterval(() => {
      timerObj.remaining--;
      renderTimerList();
      if (timerObj.remaining <= 0) {
        clearInterval(timerObj.interval);
        const idx = activeTimers.indexOf(timerObj);
        if (idx !== -1) activeTimers.splice(idx, 1);
        renderTimerList();

        // Voice alert
        voice.speak(`Alert: ${timerObj.label} countdown has expired.`);
        alert(`🔔 Karllo Alert: ${timerObj.label} expired!`);
      }
    }, 1000);

    activeTimers.push(timerObj);
    renderTimerList();
  }

  function renderTimerList() {
    activeTimerBadge.textContent = `${activeTimers.length} ACTIVE`;
    if (activeTimers.length === 0) {
      timerList.innerHTML = `<div style="color: var(--text-muted); font-size: 0.8rem; text-align: center; padding: 12px;">No active timers. Speak "Set a timer for 10 minutes".</div>`;
      return;
    }

    timerList.innerHTML = '';
    activeTimers.forEach((t) => {
      const mins = Math.floor(t.remaining / 60);
      const secs = t.remaining % 60;
      const timeStr = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

      const el = document.createElement('div');
      el.className = 'workflow-item';
      el.innerHTML = `
        <div>
          <div style="font-weight: 600;">${t.label}</div>
          <div class="workflow-countdown">${timeStr}</div>
        </div>
        <button class="hud-btn" style="padding: 2px 8px; font-size: 0.75rem; color: #ff3355;">✕</button>
      `;
      el.querySelector('button').addEventListener('click', () => {
        clearInterval(t.interval);
        const idx = activeTimers.indexOf(t);
        if (idx !== -1) activeTimers.splice(idx, 1);
        renderTimerList();
      });
      timerList.appendChild(el);
    });
  }

  // Notes List
  function renderNotesList() {
    const notes = engine.notes;
    if (notes.length === 0) {
      notesList.innerHTML = `<div style="color: var(--text-muted); font-size: 0.8rem; text-align: center; padding: 12px;">No notes recorded. Speak "Save note: [idea]".</div>`;
      return;
    }

    notesList.innerHTML = '';
    notes.forEach((n) => {
      const el = document.createElement('div');
      el.className = 'workflow-item';
      el.innerHTML = `
        <div style="flex: 1; padding-right: 8px;">
          <div style="font-size: 0.85rem;">${n.text}</div>
          <div style="font-size: 0.7rem; color: var(--text-muted);">${n.timestamp}</div>
        </div>
        <button class="hud-btn" style="padding: 2px 6px; font-size: 0.7rem; color: #ff3355;">✕</button>
      `;
      el.querySelector('button').addEventListener('click', () => {
        const idx = engine.notes.indexOf(n);
        if (idx !== -1) {
          engine.notes.splice(idx, 1);
          engine.saveNotes();
          renderNotesList();
        }
      });
      notesList.appendChild(el);
    });
  }

  // Append Chat Message
  function appendMessage(sender, text, intent) {
    const bubble = document.createElement('div');
    bubble.className = `message-bubble ${sender}`;

    const textEl = document.createElement('div');
    textEl.textContent = text;
    bubble.appendChild(textEl);

    const meta = document.createElement('div');
    meta.className = 'bubble-meta';

    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const tag = intent ? ` • ${intent}` : '';
    meta.innerHTML = `<span>${time}${tag}</span>`;

    if (sender === 'karllo') {
      const replayBtn = document.createElement('button');
      replayBtn.className = 'replay-btn';
      replayBtn.innerHTML = '🔊 Speak';
      replayBtn.addEventListener('click', () => {
        voice.speak(text);
      });
      meta.appendChild(replayBtn);
    }

    bubble.appendChild(meta);
    dialogueStream.appendChild(bubble);
    dialogueStream.scrollTop = dialogueStream.scrollHeight;
  }

  // Event Listeners for Speaking Actions
  function toggleVoiceListening() {
    if (voice.state === 'LISTENING') {
      voice.stopListening();
    } else {
      voice.startListening();
    }
  }

  micBtn.addEventListener('click', toggleVoiceListening);
  topLeftSpeakBtn.addEventListener('click', toggleVoiceListening);
  headerSpeakBtn.addEventListener('click', toggleVoiceListening);
  chatSpeakBtn.addEventListener('click', toggleVoiceListening);

  // Top Left 3 Dots Dropdown Menu Listeners
  topMenuDotsBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    topDropdownMenu.classList.toggle('active');
  });

  document.addEventListener('click', (e) => {
    if (!topDropdownMenu.contains(e.target) && e.target !== topMenuDotsBtn) {
      topDropdownMenu.classList.remove('active');
    }
  });

  menuSpeakItem.addEventListener('click', () => {
    topDropdownMenu.classList.remove('active');
    toggleVoiceListening();
  });

  menuHandsFreeItem.addEventListener('click', () => {
    topDropdownMenu.classList.remove('active');
    continuousToggleBtn.click();
  });

  menuStopSpeakingItem.addEventListener('click', () => {
    topDropdownMenu.classList.remove('active');
    voice.stopSpeaking();
  });

  menuMuteItem.addEventListener('click', () => {
    topDropdownMenu.classList.remove('active');
    muteToggleBtn.click();
  });

  menuSettingsItem.addEventListener('click', () => {
    topDropdownMenu.classList.remove('active');
    settingsModal.classList.add('active');
  });

  menuClearItem.addEventListener('click', () => {
    topDropdownMenu.classList.remove('active');
    dialogueStream.innerHTML = '';
  });

  // Send message from input field
  function submitConversationText() {
    const text = conversationTextInput.value.trim();
    if (text) {
      conversationTextInput.value = '';
      if (voice.state === 'LISTENING') {
        voice.stopListening();
      }
      handleFinalSpokenInput(text);
    }
  }

  sendMessageBtn.addEventListener('click', submitConversationText);

  conversationTextInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      submitConversationText();
    }
  });

  stopSpeakingBtn.addEventListener('click', () => {
    voice.stopSpeaking();
  });

  continuousToggleBtn.addEventListener('click', () => {
    const isNowContinuous = voice.toggleContinuous();
    continuousToggleBtn.classList.toggle('active', isNowContinuous);
    continuousToggleBtn.querySelector('#continuousIcon').textContent = isNowContinuous ? '🟢' : '🎙️';
  });

  muteToggleBtn.addEventListener('click', () => {
    const isMuted = voice.toggleMute();
    muteIcon.textContent = isMuted ? '🔇' : '🔊';
    muteToggleBtn.classList.toggle('active', isMuted);
  });

  langSelect.addEventListener('change', () => {
    voice.setLanguage(langSelect.value);
    if (voice.availableVoices.length > 0) {
      populateVoiceList(voice.availableVoices);
    }
  });

  clearChatBtn.addEventListener('click', () => {
    dialogueStream.innerHTML = '';
  });

  // Suggestion Chips
  document.querySelectorAll('.chip').forEach((chip) => {
    chip.addEventListener('click', () => {
      const q = chip.getAttribute('data-query');
      handleFinalSpokenInput(q);
    });
  });

  // Modals & Settings
  settingsModalBtn.addEventListener('click', () => {
    settingsModal.classList.add('active');
  });

  closeSettingsBtn.addEventListener('click', () => {
    settingsModal.classList.remove('active');
  });

  speechRate.addEventListener('input', () => {
    rateValue.textContent = `${speechRate.value}x`;
    voice.setVoiceParams({ rate: parseFloat(speechRate.value) });
  });

  speechPitch.addEventListener('input', () => {
    pitchValue.textContent = speechPitch.value;
    voice.setVoiceParams({ pitch: parseFloat(speechPitch.value) });
  });

  speechVolume.addEventListener('input', () => {
    volumeValue.textContent = `${Math.round(speechVolume.value * 100)}%`;
    voice.setVoiceParams({ volume: parseFloat(speechVolume.value) });
  });

  voiceSelect.addEventListener('change', () => {
    voice.setVoiceParams({ voiceName: voiceSelect.value });
  });

  testVoiceBtn.addEventListener('click', () => {
    voice.speak('Voice calibration complete. Karllo AI speech synthesis is optimal.');
  });

  // Manual Note Modal
  addNoteManualBtn.addEventListener('click', () => {
    manualNoteText.value = '';
    noteModal.classList.add('active');
  });

  closeNoteBtn.addEventListener('click', () => {
    noteModal.classList.remove('active');
  });

  saveManualNoteBtn.addEventListener('click', () => {
    const text = manualNoteText.value.trim();
    if (text) {
      engine.notes.unshift({
        id: Date.now(),
        text: text,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      });
      engine.saveNotes();
      renderNotesList();
    }
    noteModal.classList.remove('active');
  });

  // Initial renders
  renderNotesList();
  renderTimerList();
});
