import re

# Read core engine
with open('web/karllo-core.js', 'r', encoding='utf-8') as f:
    core_code = f.read().replace('export class KarlloCoreEngine', 'class KarlloCoreEngine')

# Read speech module and enhance it
with open('web/speech-module.js', 'r', encoding='utf-8') as f:
    voice_code = f.read().replace('export class KarlloVoiceModule', 'class KarlloVoiceModule')

old_listener = """    // Native Android Bridge listener
    if (typeof window !== 'undefined') {
      window.onNativeSpeechResult = (text) => {
        if (text) {
          this.onTranscript(text, true);
        }
        this.setState('IDLE');
      };
    }"""

new_listener = """    // Native Android Bridge listener with error fallback
    if (typeof window !== 'undefined') {
      window.onNativeSpeechResult = (text) => {
        if (text) {
          this.onTranscript(text, true);
        }
        this.setState('IDLE');
      };

      window.onNativeSpeechError = (errMsg) => {
        console.warn('[Karllo Voice] Native speech error:', errMsg);
        this.setState('IDLE');
        if (this.recognition && this.isSttSupported) {
          try {
            this.recognition.start();
            this.setState('LISTENING');
            return;
          } catch (e) {}
        }
        if (window.openVoiceDictationModal) {
          window.openVoiceDictationModal();
        } else {
          this.onError('Speech Recognition unavailable on this device. Please type or pick a prompt.');
        }
      };
    }"""

if old_listener in voice_code:
    voice_code = voice_code.replace(old_listener, new_listener)

# Read visualizer
with open('web/audio-visualizer.js', 'r', encoding='utf-8') as f:
    vis_code = f.read().replace('export class JarvisVisualizer', 'class JarvisVisualizer')

# Enhanced App Controller with Live Browser, Model Training & Always-Visible Input Dock
app_controller = """
document.addEventListener('DOMContentLoaded', () => {
  console.log('[Karllo AI] Initializing Voice Assistant Engine & Live Browser Interface...');
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

  // Top Left 3 Dots & Quick Speak
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

  // Conversation Dedicated Elements
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

  // Voice Dictation Fallback Modal
  const voiceDictationModal = document.getElementById('voiceDictationModal');
  const closeVoiceModalBtn = document.getElementById('closeVoiceModalBtn');
  const dictationManualInput = document.getElementById('dictationManualInput');
  const submitDictationBtn = document.getElementById('submitDictationBtn');

  window.openVoiceDictationModal = function() {
    if (voiceDictationModal) {
      voiceDictationModal.classList.add('active');
      if (dictationManualInput) dictationManualInput.focus();
    }
  };

  if (closeVoiceModalBtn) {
    closeVoiceModalBtn.addEventListener('click', () => {
      if (voiceDictationModal) voiceDictationModal.classList.remove('active');
    });
  }

  document.querySelectorAll('.voice-prompt-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      const prompt = chip.dataset.speak || chip.textContent.trim();
      if (voiceDictationModal) voiceDictationModal.classList.remove('active');
      handleFinalSpokenInput(prompt);
    });
  });

  if (submitDictationBtn && dictationManualInput) {
    submitDictationBtn.addEventListener('click', () => {
      const text = dictationManualInput.value.trim();
      if (text) {
        dictationManualInput.value = '';
        if (voiceDictationModal) voiceDictationModal.classList.remove('active');
        handleFinalSpokenInput(text);
      }
    });
    dictationManualInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        submitDictationBtn.click();
      }
    });
  }

  // Modals & Settings
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
  const addNoteManualBtn = document.getElementById('addNoteManualBtn');

  const activeTimers = [];

  // Initialize Voice Module
  const voice = new KarlloVoiceModule({
    lang: langSelect ? langSelect.value : 'en-US',
    rate: speechRate ? parseFloat(speechRate.value) : 1.0,
    pitch: speechPitch ? parseFloat(speechPitch.value) : 1.0,
    volume: speechVolume ? parseFloat(speechVolume.value) : 1.0,
    onStateChange: (state) => {
      updateHUDState(state);
    },
    onTranscript: (transcript, isFinal) => {
      if (liveTranscriptBox) liveTranscriptBox.textContent = transcript;
      if (conversationTextInput) conversationTextInput.value = transcript;
      if (isFinal) {
        if (liveTranscriptBox) liveTranscriptBox.classList.remove('interim');
        if (conversationTextInput) conversationTextInput.value = '';
        handleFinalSpokenInput(transcript);
      } else {
        if (liveTranscriptBox) liveTranscriptBox.classList.add('interim');
      }
    },
    onVoicesReady: (voices) => {
      populateVoiceList(voices);
    },
    onError: (err) => {
      console.warn('[Karllo Voice Error]', err);
      if (liveTranscriptBox) {
        liveTranscriptBox.textContent = err;
        liveTranscriptBox.classList.remove('interim');
      }
      if (listeningIndicatorBar) listeningIndicatorBar.classList.remove('active');
      setTimeout(() => {
        if (voice.state === 'IDLE' && liveTranscriptBox) {
          liveTranscriptBox.textContent = 'Press "Speak" or tap the microphone to talk with Karllo...';
        }
      }, 4000);
    }
  });

  // Visualizer
  let visualizer = null;
  if (canvas) {
    visualizer = new JarvisVisualizer(canvas, voice);
    visualizer.start();
  }

  function populateVoiceList(voices) {
    if (!voiceSelect || !voices) return;
    voiceSelect.innerHTML = '';
    const currentLang = langSelect ? langSelect.value : 'en-US';
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

  function updateHUDState(state) {
    if (statusBadge) {
      statusBadge.className = 'status-badge ' + state.toLowerCase();
      statusBadge.textContent = state;
    }

    const isListening = (state === 'LISTENING');

    if (isListening) {
      if (micBtn) { micBtn.classList.add('listening'); micBtn.innerHTML = '🛑'; micBtn.title = 'Stop Listening'; }
      if (liveTranscriptBox) liveTranscriptBox.textContent = 'Listening to your voice...';
      if (topLeftSpeakBtn) {
        topLeftSpeakBtn.classList.add('listening');
        if (topLeftSpeakIcon) topLeftSpeakIcon.textContent = '🛑';
        if (topLeftSpeakLabel) topLeftSpeakLabel.textContent = 'Stop';
      }
      if (headerSpeakBtn) {
        headerSpeakBtn.classList.add('listening');
        if (headerSpeakIcon) headerSpeakIcon.textContent = '🛑';
        if (headerSpeakText) headerSpeakText.textContent = 'Stop';
      }
      if (chatSpeakBtn) {
        chatSpeakBtn.classList.add('listening');
        if (chatSpeakIcon) chatSpeakIcon.textContent = '🛑';
        if (chatSpeakText) chatSpeakText.textContent = 'Listening...';
      }
      if (listeningIndicatorBar) listeningIndicatorBar.classList.add('active');
      if (conversationTextInput) conversationTextInput.placeholder = 'Listening... words will appear here...';
    } else {
      if (micBtn) { micBtn.classList.remove('listening'); micBtn.innerHTML = '🎙️'; micBtn.title = 'Tap to Speak'; }
      if (topLeftSpeakBtn) {
        topLeftSpeakBtn.classList.remove('listening');
        if (topLeftSpeakIcon) topLeftSpeakIcon.textContent = '🎙️';
        if (topLeftSpeakLabel) topLeftSpeakLabel.textContent = 'Speak';
      }
      if (headerSpeakBtn) {
        headerSpeakBtn.classList.remove('listening');
        if (headerSpeakIcon) headerSpeakIcon.textContent = '🎙️';
        if (headerSpeakText) headerSpeakText.textContent = 'Speak';
      }
      if (chatSpeakBtn) {
        chatSpeakBtn.classList.remove('listening');
        if (chatSpeakIcon) chatSpeakIcon.textContent = '🎙️';
        if (chatSpeakText) chatSpeakText.textContent = 'Speak';
      }
      if (listeningIndicatorBar) listeningIndicatorBar.classList.remove('active');
      if (conversationTextInput) conversationTextInput.placeholder = "Click 'Speak' or type your message here...";
    }

    if (stopSpeakingBtn) {
      if (state === 'SPEAKING') {
        stopSpeakingBtn.classList.add('visible');
      } else {
        stopSpeakingBtn.classList.remove('visible');
      }
    }
  }

  // Handle User Input Submission with Live Search Detection
  async function handleFinalSpokenInput(input) {
    if (!input || !input.trim()) return;
    const cleanText = input.trim();

    appendMessage('user', cleanText);
    updateHUDState('PROCESSING');
    
    if (liveTranscriptBox) {
      const lower = cleanText.toLowerCase();
      if (lower.includes('search') || lower.includes('browse') || lower.includes('who is') || lower.includes('what is')) {
        liveTranscriptBox.textContent = '🌐 Live browser web querying & deep analysis...';
      } else if (lower.includes('train')) {
        liveTranscriptBox.textContent = '🎓 Running neural training epoch...';
      } else {
        liveTranscriptBox.textContent = 'Neural reasoning in progress...';
      }
    }

    try {
      const response = await engine.processQuery(cleanText);
      appendMessage('karllo', response.text, response.intent);
      if (liveTranscriptBox) liveTranscriptBox.textContent = response.text.slice(0, 100) + '...';

      if (response.action) {
        handleEngineAction(response.action);
      }

      updateHUDState('SPEAKING');
      await voice.speak(response.text);
      updateHUDState('IDLE');
    } catch (err) {
      console.error('[Karllo Processing Error]', err);
      appendMessage('karllo', "I encountered an error processing your query. Please try again.");
      updateHUDState('IDLE');
    }
  }

  function handleEngineAction(action) {
    if (action.type === 'START_TIMER') {
      createTimer(action.seconds, action.label);
    } else if (action.type === 'NOTE_SAVED') {
      renderNotesList();
    } else if (action.type === 'REMINDER_SET') {
      createTimer(60, `Reminder: ${action.reminder.text}`);
    } else if (action.type === 'MODEL_TRAINED') {
      console.log('[Karllo] Model trained successfully on', action.target);
    }
  }

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
        voice.speak(`Alert: ${timerObj.label} countdown has expired.`);
        alert(`🔔 Karllo Alert: ${timerObj.label} expired!`);
      }
    }, 1000);

    activeTimers.push(timerObj);
    renderTimerList();
  }

  const activeTimerBadge = document.getElementById('activeTimerBadge');
  const timerList = document.getElementById('timerList');

  function renderTimerList() {
    if (!activeTimerBadge || !timerList) return;
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

      const card = document.createElement('div');
      card.className = 'workflow-card';
      card.innerHTML = `
        <div class="wf-info">
          <div class="wf-title">${t.label}</div>
          <div class="wf-meta">${timeStr} remaining</div>
        </div>
        <button class="wf-action-btn cancel-timer-btn" data-id="${t.id}">Cancel</button>
      `;

      card.querySelector('.cancel-timer-btn').addEventListener('click', () => {
        clearInterval(t.interval);
        const idx = activeTimers.indexOf(t);
        if (idx !== -1) activeTimers.splice(idx, 1);
        renderTimerList();
      });

      timerList.appendChild(card);
    });
  }

  const notesList = document.getElementById('notesList');
  function renderNotesList() {
    if (!notesList) return;
    if (engine.notes.length === 0) {
      notesList.innerHTML = `<div style="color: var(--text-muted); font-size: 0.8rem; text-align: center; padding: 12px;">No saved notes. Speak "Save note: buy groceries".</div>`;
      return;
    }

    notesList.innerHTML = '';
    engine.notes.slice(-5).reverse().forEach((n) => {
      const card = document.createElement('div');
      card.className = 'workflow-card';
      const time = new Date(n.timestamp).toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
      card.innerHTML = `
        <div class="wf-info">
          <div class="wf-title">${n.text}</div>
          <div class="wf-meta">${time}</div>
        </div>
        <button class="wf-action-btn delete-note-btn" data-id="${n.id}">✕</button>
      `;
      card.querySelector('.delete-note-btn').addEventListener('click', () => {
        engine.deleteNote(n.id);
        renderNotesList();
      });
      notesList.appendChild(card);
    });
  }

  // Append Chat Message with Rich Markdown & Logo Avatar
  function appendMessage(sender, text, intent) {
    if (!dialogueStream) return;
    const bubble = document.createElement('div');
    bubble.className = `message-bubble ${sender}`;

    // Avatar
    const avatarWrapper = document.createElement('div');
    avatarWrapper.className = 'msg-avatar-wrapper';
    if (sender === 'karllo') {
      avatarWrapper.innerHTML = `<img src="logo.png" class="msg-avatar karllo-avatar" alt="Karllo Neural">`;
    } else {
      avatarWrapper.innerHTML = `<div class="msg-avatar user-avatar">👤</div>`;
    }
    bubble.appendChild(avatarWrapper);

    // Message Body
    const contentBox = document.createElement('div');
    contentBox.className = 'bubble-content-box';

    const textEl = document.createElement('div');
    textEl.className = 'bubble-text';

    // Rich markdown parsing
    let formattedText = text;
    formattedText = formattedText.replace(/\\*\\*(.*?)\\*\\*/g, '<strong>$1</strong>');
    formattedText = formattedText.replace(/\\[([^\\]]+)\\]\\((https?:\\/\\/[^\\)]+)\\)/g, '<a href="$2" target="_blank" rel="noopener" class="live-web-link">🔗 $1</a>');
    formattedText = formattedText.replace(/### (.*?)(?:\\n|<br>|$)/g, '<div class="msg-subheading">$1</div>');
    formattedText = formattedText.replace(/\\n/g, '<br>');

    textEl.innerHTML = formattedText;
    contentBox.appendChild(textEl);

    // Metadata & Replay
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

    contentBox.appendChild(meta);
    bubble.appendChild(contentBox);

    dialogueStream.appendChild(bubble);
    dialogueStream.scrollTop = dialogueStream.scrollHeight;
  }

  // Event Listeners for Speaking Actions
  function toggleVoiceListening() {
    console.log('[Karllo Voice] Toggle listening called, current state:', voice.state);
    if (voice.state === 'LISTENING') {
      voice.stopListening();
    } else {
      voice.startListening().then(success => {
        if (!success) {
          if (window.openVoiceDictationModal) window.openVoiceDictationModal();
        }
      }).catch(err => {
        if (window.openVoiceDictationModal) window.openVoiceDictationModal();
      });
    }
  }

  if (micBtn) micBtn.addEventListener('click', toggleVoiceListening);
  if (topLeftSpeakBtn) topLeftSpeakBtn.addEventListener('click', toggleVoiceListening);
  if (headerSpeakBtn) headerSpeakBtn.addEventListener('click', toggleVoiceListening);
  if (chatSpeakBtn) chatSpeakBtn.addEventListener('click', toggleVoiceListening);

  // Top Left 3 Dots Dropdown Menu Listeners
  if (topMenuDotsBtn && topDropdownMenu) {
    topMenuDotsBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      topDropdownMenu.classList.toggle('active');
    });

    document.addEventListener('click', (e) => {
      if (!topDropdownMenu.contains(e.target) && e.target !== topMenuDotsBtn) {
        topDropdownMenu.classList.remove('active');
      }
    });

    if (menuSpeakItem) {
      menuSpeakItem.addEventListener('click', () => {
        topDropdownMenu.classList.remove('active');
        toggleVoiceListening();
      });
    }

    if (menuHandsFreeItem) {
      menuHandsFreeItem.addEventListener('click', () => {
        topDropdownMenu.classList.remove('active');
        if (continuousToggleBtn) continuousToggleBtn.click();
      });
    }

    if (menuStopSpeakingItem) {
      menuStopSpeakingItem.addEventListener('click', () => {
        topDropdownMenu.classList.remove('active');
        voice.stopSpeaking();
      });
    }

    if (menuMuteItem) {
      menuMuteItem.addEventListener('click', () => {
        topDropdownMenu.classList.remove('active');
        if (muteToggleBtn) muteToggleBtn.click();
      });
    }

    if (menuSettingsItem && settingsModal) {
      menuSettingsItem.addEventListener('click', () => {
        topDropdownMenu.classList.remove('active');
        settingsModal.classList.add('active');
      });
    }

    if (menuClearItem && dialogueStream) {
      menuClearItem.addEventListener('click', () => {
        topDropdownMenu.classList.remove('active');
        dialogueStream.innerHTML = '';
      });
    }
  }

  // Send message from input field
  function submitConversationText() {
    if (!conversationTextInput) return;
    const text = conversationTextInput.value.trim();
    if (text) {
      conversationTextInput.value = '';
      if (voice.state === 'LISTENING') {
        voice.stopListening();
      }
      handleFinalSpokenInput(text);
    }
  }

  if (sendMessageBtn) sendMessageBtn.addEventListener('click', submitConversationText);

  if (conversationTextInput) {
    conversationTextInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        submitConversationText();
      }
    });

    // Auto scroll into view when virtual keyboard pops up
    conversationTextInput.addEventListener('focus', () => {
      setTimeout(() => {
        conversationTextInput.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }, 200);
    });
  }

  if (stopSpeakingBtn) {
    stopSpeakingBtn.addEventListener('click', () => {
      voice.stopSpeaking();
    });
  }

  if (continuousToggleBtn) {
    continuousToggleBtn.addEventListener('click', () => {
      const isNowContinuous = voice.toggleContinuous();
      continuousToggleBtn.classList.toggle('active', isNowContinuous);
      const icon = continuousToggleBtn.querySelector('#continuousIcon');
      if (icon) icon.textContent = isNowContinuous ? '🟢' : '🎙️';
    });
  }

  if (muteToggleBtn) {
    muteToggleBtn.addEventListener('click', () => {
      const isMuted = voice.toggleMute();
      if (muteIcon) muteIcon.textContent = isMuted ? '🔇' : '🔊';
      muteToggleBtn.classList.toggle('active', isMuted);
    });
  }

  if (langSelect) {
    langSelect.addEventListener('change', () => {
      voice.setLanguage(langSelect.value);
      if (voice.availableVoices.length > 0) {
        populateVoiceList(voice.availableVoices);
      }
    });
  }

  if (clearChatBtn && dialogueStream) {
    clearChatBtn.addEventListener('click', () => {
      dialogueStream.innerHTML = '';
    });
  }

  // Suggestion Chips
  document.querySelectorAll('.chip').forEach((chip) => {
    chip.addEventListener('click', () => {
      const query = chip.dataset.query;
      handleFinalSpokenInput(query);
    });
  });

  // Settings Modal Controls
  if (settingsModalBtn && settingsModal) {
    settingsModalBtn.addEventListener('click', () => {
      settingsModal.classList.add('active');
    });
  }
  if (closeSettingsBtn && settingsModal) {
    closeSettingsBtn.addEventListener('click', () => {
      settingsModal.classList.remove('active');
    });
  }

  if (speechRate) {
    speechRate.addEventListener('input', () => {
      const val = parseFloat(speechRate.value);
      if (rateValue) rateValue.textContent = `${val}x`;
      voice.setRate(val);
    });
  }

  if (speechPitch) {
    speechPitch.addEventListener('input', () => {
      const val = parseFloat(speechPitch.value);
      if (pitchValue) pitchValue.textContent = `${val}x`;
      voice.setPitch(val);
    });
  }

  if (speechVolume) {
    speechVolume.addEventListener('input', () => {
      const val = parseFloat(speechVolume.value);
      if (volumeValue) volumeValue.textContent = `${Math.round(val * 100)}%`;
      voice.setVolume(val);
    });
  }

  if (testVoiceBtn) {
    testVoiceBtn.addEventListener('click', () => {
      if (voiceSelect && voiceSelect.value) {
        voice.setVoiceByName(voiceSelect.value);
      }
      voice.speak("Hello! Karllo AI neural voice synthesis and live web intelligence are operational.");
    });
  }

  // Notes Modal Controls
  if (addNoteManualBtn && noteModal) {
    addNoteManualBtn.addEventListener('click', () => {
      noteModal.classList.add('active');
      if (manualNoteText) manualNoteText.focus();
    });
  }
  if (closeNoteBtn && noteModal) {
    closeNoteBtn.addEventListener('click', () => {
      noteModal.classList.remove('active');
    });
  }
  if (saveManualNoteBtn && manualNoteText) {
    saveManualNoteBtn.addEventListener('click', () => {
      const text = manualNoteText.value.trim();
      if (text) {
        engine.addNote(text);
        manualNoteText.value = '';
        if (noteModal) noteModal.classList.remove('active');
        renderNotesList();
      }
    });
  }

  // Initial lists render
  renderTimerList();
  renderNotesList();
  console.log('[Karllo AI] Live browser interface & neural engine ready.');
});
"""

full_bundle = f"""/**
 * Karllo AI - Consolidated Unified Script Bundle
 * Live Browser Access, Deep Semantic Analysis & Model Training Engine
 */

window.addEventListener('error', function(e) {{
  console.error('[Karllo Runtime Error]', e.message, e.filename, e.lineno);
}});

{core_code}

{voice_code}

{vis_code}

{app_controller}
"""

with open('web/bundle.js', 'w', encoding='utf-8') as f:
    f.write(full_bundle)

print('Updated bundle.js with live browser & model training. Size:', len(full_bundle))
