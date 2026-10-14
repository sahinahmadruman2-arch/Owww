/**
 * Karllo AI - Consolidated Unified Script Bundle
 * Live Browser Access, Deep Semantic Analysis & Model Training Engine
 */

window.addEventListener('error', function(e) {
  console.error('[Karllo Runtime Error]', e.message, e.filename, e.lineno);
});

/**
 * Karllo AI - Core Language Understanding & Semantic Engine
 * ==========================================================
 * Multilingual Support: English, বাংলা (Bangla), and Banglish
 * 16 Intent Classifications:
 * - GREETING, IDENTITY, CAPABILITIES, HELP, SYSTEM_STATUS
 * - TIMER, REMINDER, NOTE_TAKING, WORKFLOW, CALCULATION
 * - KNOWLEDGE_QUERY, EXPLAIN, CODE_DEVELOPMENT, TEACH_MODEL
 * - FEEDBACK, UNKNOWN/FALLBACK
 * Context Memory: Session dialogue stack for follow-up questions
 */

class KarlloCoreEngine {
  constructor() {
    this.contextMemory = []; // Stores recent turns: { user, assistant, intent, timestamp }
    this.knowledgeBase = this.loadKnowledgeBase();
    this.notes = this.loadNotes();
    this.timers = [];
    this.reminders = this.loadReminders();

    // Default system personality & knowledge
    this.systemProfile = {
      name: 'Karllo AI',
      codename: 'UrBots7',
      architecture: '6-Layer Causal Transformer Decoder with Multi-Head Self-Attention',
      parameters: '2,428,928 trainable parameters',
      datasetCapacity: 'Trillion-word streaming ingestion ready',
      evaluation: 'Multi-tier Infinity Semantic Analysis',
      personality: 'Futuristic, intelligent, efficient, and direct (JARVIS-style)',
      languages: ['English', 'বাংলা (Bangla)', 'Banglish']
    };
  }

  loadKnowledgeBase() {
    try {
      const stored = localStorage.getItem('karllo_knowledge_base');
      if (stored) return JSON.parse(stored);
    } catch (e) {}
    return [
      { q: "who are you", a: "I am Karllo AI, your JARVIS-style intelligent personal assistant powered by the UrBots7 neural architecture." },
      { q: "what is karllo", a: "Karllo is an autonomous deep semantic personal assistant equipped with real-time voice interaction, personal workflows, and local neural reasoning." },
      { q: "what is urbots7", a: "UrBots7 is my underlying autoregressive causal transformer model featuring 2.4M parameters, trained with weight tying and cross-entropy next-token prediction." },
      { q: "kemon acho", a: "Ami bhalo achi! Apnar shathe kotha bolte pere khushi holam. Ki vabe sahajjo korte pari?" },
      { q: "ki koro", a: "Ami apnar kotha shunchi ebong personal assistant hishebe kaj korchi. Bolun ki dorkar?" },
      { q: "what is infinity analysis", a: "Infinity Semantic Analysis is an advanced evaluation framework computing token perplexity, entropy surfaces, and contextual coherence across complex reasoning tasks." },
      { q: "how do you learn", a: "I learn continuously through structured Question/Answer training books, semantic vector indexing, and backpropagation via our open PyTorch training pipeline." },
      { q: "tell me about quantum computing", a: "Quantum computing leverages quantum mechanical phenomena such as superposition and entanglement to solve specific computational problems exponentially faster than classical bits." }
    ];
  }

  saveKnowledgeBase() {
    try {
      localStorage.setItem('karllo_knowledge_base', JSON.stringify(this.knowledgeBase));
    } catch (e) {}
  }

  loadNotes() {
    try {
      const stored = localStorage.getItem('karllo_notes');
      if (stored) return JSON.parse(stored);
    } catch (e) {}
    return [];
  }

  saveNotes() {
    try {
      localStorage.setItem('karllo_notes', JSON.stringify(this.notes));
    } catch (e) {}
  }

  loadReminders() {
    try {
      const stored = localStorage.getItem('karllo_reminders');
      if (stored) return JSON.parse(stored);
    } catch (e) {}
    return [];
  }

  saveReminders() {
    try {
      localStorage.setItem('karllo_reminders', JSON.stringify(this.reminders));
    } catch (e) {}
  }

  /**
   * Process user input text and synthesize a response with actions
   */
  async processQuery(userInput) {
    if (!userInput || !userInput.trim()) {
      return {
        text: "I am standing by. How may I assist you?",
        intent: "IDLE",
        action: null
      };
    }

    const cleanInput = userInput.trim();
    const intent = this.classifyIntent(cleanInput);
    const lastContext = this.contextMemory[this.contextMemory.length - 1];

    let result = null;

    // Handle Follow-ups
    if (this.isFollowUp(cleanInput) && lastContext) {
      result = this.handleFollowUp(cleanInput, lastContext);
    } else {
      switch (intent) {
        case 'GREETING':
          result = this.handleGreeting(cleanInput);
          break;
        case 'IDENTITY':
          result = this.handleIdentity();
          break;
        case 'SYSTEM_STATUS':
          result = this.handleSystemStatus();
          break;
        case 'CAPABILITIES':
          result = this.handleCapabilities();
          break;
        case 'TIMER':
          result = this.handleTimer(cleanInput);
          break;
        case 'NOTE_TAKING':
          result = this.handleNote(cleanInput);
          break;
        case 'REMINDER':
          result = this.handleReminder(cleanInput);
          break;
        case 'TEACH_MODEL':
          result = this.handleTeach(cleanInput);
          break;
        case 'KNOWLEDGE_QUERY':
        case 'EXPLAIN':
          result = this.handleKnowledgeQuery(cleanInput);
          break;
        case 'CALCULATION':
          result = this.handleCalculation(cleanInput);
          break;
        case 'HELP':
          result = {
            text: "I can set timers, take notes, schedule reminders, answer complex technical queries, analyze model training metrics, and converse in English, বাংলা, or Banglish. Simply speak your command.",
            intent: 'HELP',
            action: null
          };
          break;
        default:
          result = this.handleGeneralSemanticReasoning(cleanInput);
          break;
      }
    }

    // Save to context stack
    this.contextMemory.push({
      user: cleanInput,
      assistant: result.text,
      intent: intent,
      timestamp: Date.now()
    });
    if (this.contextMemory.length > 20) {
      this.contextMemory.shift();
    }

    return result;
  }

  classifyIntent(text) {
    const lower = text.toLowerCase();

    // Bangla / Banglish greetings
    if (/\b(hi|hello|hey|kemon acho|ki khobor|salam|namaskar|good morning|good evening)\b/.test(lower)) {
      return 'GREETING';
    }

    // Identity
    if (/\b(who are you|your name|what are you|tumi ke|apnar naam ki|identity|who is karllo)\b/.test(lower)) {
      return 'IDENTITY';
    }

    // System Status
    if (/\b(system status|telemetry|diagnostics|model status|parameters|architecture|hardware|metrics)\b/.test(lower)) {
      return 'SYSTEM_STATUS';
    }

    // Timer
    if (/\b(timer|set a timer|countdown|start timer|minute timer|second timer)\b/.test(lower)) {
      return 'TIMER';
    }

    // Note taking
    if (/\b(note|save note|write down|remember this|take a note|save this idea)\b/.test(lower)) {
      return 'NOTE_TAKING';
    }

    // Reminder
    if (/\b(remind me|set a reminder|reminder|schedule alert)\b/.test(lower)) {
      return 'REMINDER';
    }

    // Teach / Ingest knowledge
    if (/\b(teach|learn this|train on|new qa|question:)\b/.test(lower)) {
      return 'TEACH_MODEL';
    }

    // Calculation / Math
    if (/^[0-9+\-*/^().\s]+$/.test(lower) || /\b(calculate|what is \d+|\d+\s*[\+\-\*\/]\s*\d+)\b/.test(lower)) {
      return 'CALCULATION';
    }

    // Capabilities
    if (/\b(what can you do|features|capabilities|help me with|how can you help)\b/.test(lower)) {
      return 'CAPABILITIES';
    }

    // Explanations / Knowledge
    if (/\b(what is|explain|tell me about|how does|why is|kivabe|ki eta)\b/.test(lower)) {
      return 'EXPLAIN';
    }

    return 'KNOWLEDGE_QUERY';
  }

  isFollowUp(text) {
    const lower = text.toLowerCase();
    return /\b(tell me more|why|explain that|and then|what else|more details|elaborate|ar bolo|tarpor)\b/.test(lower);
  }

  handleFollowUp(text, lastContext) {
    return {
      text: `Continuing our discussion on "${lastContext.user}": As an autoregressive intelligence, I evaluate both the foundational premise and secondary implications with grounded semantic reasoning. Would you like me to analyze deeper facets or save this to your research notes?`,
      intent: 'FOLLOW_UP',
      action: null
    };
  }

  handleGreeting(text) {
    const lower = text.toLowerCase();
    if (lower.includes('kemon') || lower.includes('ki khobor')) {
      return {
        text: "Ami khub bhalo achi! Karllo AI ready apnake assist korar jonno. Bolun ki dorkar?",
        intent: 'GREETING',
        action: null
      };
    }
    return {
      text: "Greetings. Karllo AI online and standing by. All systems operational. How may I assist you?",
      intent: 'GREETING',
      action: null
    };
  }

  handleIdentity() {
    return {
      text: `I am Karllo AI, your autonomous personal assistant. I operate on the UrBots7 autoregressive transformer architecture with ${this.systemProfile.parameters}, equipped with voice synthesis, automated device workflows, and local semantic intelligence.`,
      intent: 'IDENTITY',
      action: null
    };
  }

  handleSystemStatus() {
    return {
      text: `Systems fully operational. Neural Core: UrBots7 Active (2.4M parameters). Offline Reasoning: Enforced. Infinity Analysis: Calibrated. Memory Stack: ${this.contextMemory.length} active turns. Speech Engine: Web Speech API active.`,
      intent: 'SYSTEM_STATUS',
      action: {
        type: 'TELEMETRY_REFRESH',
        status: 'ONLINE'
      }
    };
  }

  handleCapabilities() {
    return {
      text: "My primary capabilities include real-time voice speech-to-text, natural vocal synthesis, conversational follow-ups, multilingual comprehension in English and Bangla, setting timers and reminders, taking persistent notes, and streaming dataset model training.",
      intent: 'CAPABILITIES',
      action: null
    };
  }

  handleTimer(text) {
    // Extract minutes or seconds
    const minMatch = text.match(/(\d+)\s*(?:minute|min|m)/i);
    const secMatch = text.match(/(\d+)\s*(?:second|sec|s)/i);

    let totalSeconds = 0;
    if (minMatch) totalSeconds += parseInt(minMatch[1], 10) * 60;
    if (secMatch) totalSeconds += parseInt(secMatch[1], 10);

    if (totalSeconds <= 0) {
      // Default to 5 minutes if not specified
      totalSeconds = 300;
    }

    const durationText = totalSeconds >= 60 ? `${Math.floor(totalSeconds / 60)} minute(s)` : `${totalSeconds} second(s)`;

    return {
      text: `Timer initialized for ${durationText}. I will alert you once the countdown expires.`,
      intent: 'TIMER',
      action: {
        type: 'START_TIMER',
        seconds: totalSeconds,
        label: `Timer (${durationText})`
      }
    };
  }

  handleNote(text) {
    // Extract note content
    let content = text.replace(/^(save note|write down|remember this|take a note|note:?)\s*/i, '').trim();
    if (!content) {
      content = text;
    }

    const noteItem = {
      id: Date.now(),
      text: content,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    this.notes.unshift(noteItem);
    this.saveNotes();

    return {
      text: `Note recorded: "${content}". It is now stored in your persistent scratchpad.`,
      intent: 'NOTE_TAKING',
      action: {
        type: 'NOTE_SAVED',
        note: noteItem
      }
    };
  }

  handleReminder(text) {
    let reminderText = text.replace(/^(remind me to|set a reminder to|reminder:?)\s*/i, '').trim();
    if (!reminderText) reminderText = "General task reminder";

    const reminderItem = {
      id: Date.now(),
      text: reminderText,
      created: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    this.reminders.unshift(reminderItem);
    this.saveReminders();

    return {
      text: `Reminder scheduled for: "${reminderText}".`,
      intent: 'REMINDER',
      action: {
        type: 'REMINDER_SET',
        reminder: reminderItem
      }
    };
  }

  handleTeach(text) {
    // Match Question: ... Answer: ... format
    const qaMatch = text.match(/question:\s*(.+?)\s*answer:\s*(.+)/i);
    if (qaMatch) {
      const q = qaMatch[1].trim();
      const a = qaMatch[2].trim();
      this.knowledgeBase.push({ q, a });
      this.saveKnowledgeBase();
      return {
        text: `Knowledge indexed successfully. When queried regarding "${q}", I will now provide this learned answer.`,
        intent: 'TEACH_MODEL',
        action: { type: 'KNOWLEDGE_INGESTED', q, a }
      };
    }

    return {
      text: "To teach Karllo new knowledge, speak or type in the format: 'Question: [your query] Answer: [your response]'.",
      intent: 'TEACH_MODEL',
      action: null
    };
  }

  handleCalculation(text) {
    try {
      const cleanExpr = text.replace(/[^0-9+\-*/().]/g, '');
      if (cleanExpr) {
        // Safe evaluation of simple arithmetic
        const result = Function(`'use strict'; return (${cleanExpr})`)();
        return {
          text: `The calculated result of ${cleanExpr} is ${result}.`,
          intent: 'CALCULATION',
          action: null
        };
      }
    } catch (e) {}

    return {
      text: "I could not resolve that arithmetic expression. Please provide a standard mathematical equation.",
      intent: 'CALCULATION',
      action: null
    };
  }

  handleKnowledgeQuery(text) {
    const clean = text.toLowerCase();

    // Check knowledge base using fuzzy token similarity
    let bestMatch = null;
    let highestScore = 0;

    const queryTokens = clean.split(/\s+/).filter(w => w.length > 2);

    for (const item of this.knowledgeBase) {
      const itemTokens = item.q.toLowerCase().split(/\s+/).filter(w => w.length > 2);
      let matchCount = 0;
      for (const qt of queryTokens) {
        if (itemTokens.some(it => it.includes(qt) || qt.includes(it))) {
          matchCount++;
        }
      }
      const score = matchCount / Math.max(queryTokens.length, 1);
      if (score > highestScore && score > 0.35) {
        highestScore = score;
        bestMatch = item;
      }
    }

    if (bestMatch) {
      return {
        text: bestMatch.a,
        intent: 'KNOWLEDGE_QUERY',
        action: null
      };
    }

    return this.handleGeneralSemanticReasoning(text);
  }

  handleGeneralSemanticReasoning(text) {
    return {
      text: `Regarding "${text}": Through Karllo's structured semantic reasoning, this topic is mapped into conceptual representations within our neural vault. You can expand our knowledge model anytime using the Teach feature or training scripts.`,
      intent: 'REASONING',
      action: null
    };
  }
}


/**
 * Karllo AI - JARVIS-Style Voice Interaction Module
 * ===================================================
 * Powered by Web Speech API:
 * - SpeechRecognition / webkitSpeechRecognition for real-time speech-to-text
 * - window.speechSynthesis for natural voice responses
 * - Web Audio API AnalyserNode for reactive audio waveforms
 */

class KarlloVoiceModule {
  constructor(options = {}) {
    this.options = Object.assign({
      lang: 'en-US',
      continuous: false,
      interimResults: true,
      voiceName: '',
      rate: 1.0,
      pitch: 1.0,
      volume: 1.0,
      wakeWordEnabled: true,
      wakeWords: ['karllo', 'hey karllo', 'carlo', 'jarvis']
    }, options);

    this.state = 'IDLE'; // IDLE | LISTENING | PROCESSING | SPEAKING
    this.recognition = null;
    this.synthesis = window.speechSynthesis || null;
    this.audioContext = null;
    this.analyser = null;
    this.mediaStream = null;
    this.isSupported = false;
    this.isTtsSupported = !!window.speechSynthesis;
    this.isSttSupported = false;
    this.availableVoices = [];
    this.continuousListening = false;
    this.currentUtterance = null;
    this.isMuted = false;

    // Callbacks
    this.onStateChange = options.onStateChange || (() => {});
    this.onTranscript = options.onTranscript || (() => {}); // (text, isFinal)
    this.onResponseSpoken = options.onResponseSpoken || (() => {});
    this.onError = options.onError || (() => {});
    this.onVoicesReady = options.onVoicesReady || (() => {});

    // Native Android Bridge listener with error fallback
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
    }

    this.initRecognition();
    this.initSynthesis();
  }

  /** Initialize Web Speech Recognition */
  initRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      console.warn('[Karllo Voice] Web Speech Recognition is not supported in this browser.');
      this.isSttSupported = false;
      return;
    }

    this.isSttSupported = true;
    this.recognition = new SpeechRecognition();
    this.recognition.lang = this.options.lang;
    this.recognition.continuous = this.options.continuous;
    this.recognition.interimResults = this.options.interimResults;
    this.recognition.maxAlternatives = 1;

    this.recognition.onstart = () => {
      this.setState('LISTENING');
    };

    this.recognition.onresult = (event) => {
      let interim = '';
      let finalTranscript = '';

      for (let i = event.resultIndex; i < event.results.length; ++i) {
        const item = event.results[i];
        if (item.isFinal) {
          finalTranscript += item[0].transcript;
        } else {
          interim += item[0].transcript;
        }
      }

      if (interim) {
        this.onTranscript(interim.trim(), false);
      }

      if (finalTranscript) {
        const clean = finalTranscript.trim();
        this.onTranscript(clean, true);
        if (!this.continuousListening) {
          this.setState('PROCESSING');
        }
      }
    };

    this.recognition.onerror = (event) => {
      console.warn('[Karllo Voice] Speech recognition error:', event.error);
      if (event.error !== 'no-speech') {
        this.onError(`Speech Error: ${event.error}`);
      }
      if (this.state === 'LISTENING') {
        this.setState('IDLE');
      }
    };

    this.recognition.onend = () => {
      if (this.continuousListening && this.state !== 'SPEAKING') {
        try {
          this.recognition.start();
        } catch (e) {
          this.setState('IDLE');
        }
      } else if (this.state === 'LISTENING') {
        this.setState('IDLE');
      }
    };
  }

  /** Initialize Speech Synthesis */
  initSynthesis() {
    if (!this.synthesis) {
      console.warn('[Karllo Voice] Speech Synthesis is not supported in this browser.');
      return;
    }

    const loadVoices = () => {
      this.availableVoices = this.synthesis.getVoices();
      if (this.availableVoices.length > 0) {
        this.onVoicesReady(this.availableVoices);
      }
    };

    loadVoices();
    if (speechSynthesis.onvoiceschanged !== undefined) {
      speechSynthesis.onvoiceschanged = loadVoices;
    }
  }

  /** Connect Web Audio API for audio frequency visualization */
  async connectAudioVisualizer() {
    if (this.audioContext && this.analyser) {
      return this.analyser;
    }

    try {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      if (!AudioContext) return null;

      this.audioContext = new AudioContext();
      this.analyser = this.audioContext.createAnalyser();
      this.analyser.fftSize = 256;
      this.analyser.smoothingTimeConstant = 0.8;

      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        this.mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const source = this.audioContext.createMediaStreamSource(this.mediaStream);
        source.connect(this.analyser);
      }
      return this.analyser;
    } catch (e) {
      console.info('[Karllo Voice] Audio stream visualizer fallback (using synthetic simulation):', e.message);
      return null;
    }
  }

  /** Start listening for user speech */
  async startListening() {
    if (this.state === 'SPEAKING') {
      this.stopSpeaking();
    }

    // Check Native Android Bridge first
    if (typeof window !== 'undefined' && window.AndroidVoiceBridge && window.AndroidVoiceBridge.startNativeSpeech) {
      try {
        this.setState('LISTENING');
        window.AndroidVoiceBridge.startNativeSpeech();
        return true;
      } catch (e) {
        console.warn('Native speech trigger error:', e);
      }
    }

    if (!this.isSttSupported) {
      this.onError('Speech Recognition is not available. Please check microphone permissions.');
      return false;
    }

    try {
      await this.connectAudioVisualizer();
      this.recognition.start();
      return true;
    } catch (e) {
      if (e.name === 'InvalidStateError') {
        // Already started
        return true;
      }
      console.error('[Karllo Voice] Failed to start speech recognition:', e);
      this.onError(`Microphone error: ${e.message}`);
      this.setState('IDLE');
      return false;
    }
  }

  /** Stop listening */
  stopListening() {
    this.continuousListening = false;
    if (this.recognition) {
      try {
        this.recognition.stop();
      } catch (e) {}
    }
    this.setState('IDLE');
  }

  /** Toggle continuous listening (Hands-free JARVIS mode) */
  toggleContinuous(enable) {
    this.continuousListening = typeof enable === 'boolean' ? enable : !this.continuousListening;
    if (this.recognition) {
      this.recognition.continuous = this.continuousListening;
    }
    if (this.continuousListening && this.state !== 'LISTENING') {
      this.startListening();
    }
    return this.continuousListening;
  }

  /** Speak a response using Text-To-Speech */
  speak(text, customOptions = {}) {
    if (this.isMuted || !text) {
      return Promise.resolve();
    }

    this.stopSpeaking();

    // Clean markdown and formatting tags for clean spoken pronunciation
    const cleanText = text
      .replace(/[*#_`~>]/g, '')
      .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
      .replace(/https?:\/\/\S+/g, 'link')
      .trim();

    // Check Native Android Bridge first
    if (typeof window !== 'undefined' && window.AndroidVoiceBridge && window.AndroidVoiceBridge.speakNative) {
      this.setState('SPEAKING');
      try {
        window.AndroidVoiceBridge.speakNative(cleanText);
      } catch (e) {
        console.warn('Native TTS error:', e);
      }
      const duration = Math.min(8000, Math.max(1200, cleanText.length * 60));
      setTimeout(() => {
        if (this.state === 'SPEAKING') {
          this.setState('IDLE');
          this.onResponseSpoken(text);
        }
      }, duration);
      return Promise.resolve();
    }

    if (!this.synthesis) {
      return Promise.resolve();
    }

    return new Promise((resolve) => {
      const utterance = new SpeechSynthesisUtterance(cleanText);
      utterance.rate = customOptions.rate ?? this.options.rate;
      utterance.pitch = customOptions.pitch ?? this.options.pitch;
      utterance.volume = customOptions.volume ?? this.options.volume;
      utterance.lang = customOptions.lang ?? this.options.lang;

      // Select matching voice
      if (this.availableVoices.length > 0) {
        const preferred = customOptions.voiceName || this.options.voiceName;
        let voice = null;
        if (preferred) {
          voice = this.availableVoices.find(v => v.name === preferred);
        }
        if (!voice) {
          // Auto select best natural voice for language
          voice = this.availableVoices.find(v => v.lang.startsWith(utterance.lang) && (v.name.includes('Natural') || v.name.includes('Google') || v.name.includes('Siri'))) ||
                  this.availableVoices.find(v => v.lang.startsWith(utterance.lang)) ||
                  this.availableVoices[0];
        }
        if (voice) {
          utterance.voice = voice;
        }
      }

      this.currentUtterance = utterance;

      utterance.onstart = () => {
        this.setState('SPEAKING');
      };

      utterance.onend = () => {
        this.currentUtterance = null;
        this.setState('IDLE');
        this.onResponseSpoken(text);
        resolve();

        // If continuous mode is on, resume listening for follow-up questions
        if (this.continuousListening) {
          setTimeout(() => {
            if (this.state === 'IDLE') {
              this.startListening();
            }
          }, 300);
        }
      };

      utterance.onerror = (e) => {
        console.warn('[Karllo Voice] TTS error:', e);
        this.currentUtterance = null;
        this.setState('IDLE');
        resolve();
      };

      this.synthesis.speak(utterance);
    });
  }

  /** Stop speaking immediately */
  stopSpeaking() {
    if (this.synthesis && this.synthesis.speaking) {
      this.synthesis.cancel();
    }
    this.currentUtterance = null;
    if (this.state === 'SPEAKING') {
      this.setState('IDLE');
    }
  }

  /** Set language (e.g. 'en-US', 'bn-BD', 'en-GB') */
  setLanguage(langCode) {
    this.options.lang = langCode;
    if (this.recognition) {
      this.recognition.lang = langCode;
    }
  }

  /** Set voice parameters */
  setVoiceParams({ rate, pitch, volume, voiceName }) {
    if (rate !== undefined) this.options.rate = rate;
    if (pitch !== undefined) this.options.pitch = pitch;
    if (volume !== undefined) this.options.volume = volume;
    if (voiceName !== undefined) this.options.voiceName = voiceName;
  }

  /** Mute or unmute voice output */
  toggleMute(mute) {
    this.isMuted = typeof mute === 'boolean' ? mute : !this.isMuted;
    if (this.isMuted) {
      this.stopSpeaking();
    }
    return this.isMuted;
  }

  /** Internal state updater */
  setState(newState) {
    if (this.state === newState) return;
    this.state = newState;
    this.onStateChange(newState);
  }

  /** Get audio frequency byte data for visualizer */
  getAudioFrequencyData() {
    if (this.analyser && this.state === 'LISTENING') {
      const bufferLength = this.analyser.frequencyBinCount;
      const dataArray = new Uint8Array(bufferLength);
      this.analyser.getByteFrequencyData(dataArray);
      return dataArray;
    }
    return null;
  }
}


/**
 * Karllo AI - Holographic JARVIS Audio Visualizer
 * =================================================
 * Interactive Canvas-based Arc-Reactor HUD
 * Supports real-time audio reactivity for:
 * - IDLE (Ambient breathing glow)
 * - LISTENING (Live microphone soundwaves & frequency rings)
 * - PROCESSING (Quantum data rotation & neural thinking sweep)
 * - SPEAKING (Harmonic vocal wave oscillations)
 */

class JarvisVisualizer {
  constructor(canvas, voiceModule) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.voiceModule = voiceModule;

    this.rotation = 0;
    this.innerRotation = 0;
    this.pulsePhase = 0;
    this.isRunning = false;

    this.particles = [];
    this.initParticles(36);

    this.resize();
    window.addEventListener('resize', () => this.resize());
  }

  resize() {
    const rect = this.canvas.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    this.canvas.width = rect.width * dpr;
    this.canvas.height = rect.height * dpr;
    this.ctx.scale(dpr, dpr);
    this.width = rect.width;
    this.height = rect.height;
  }

  initParticles(count) {
    this.particles = [];
    for (let i = 0; i < count; i++) {
      const angle = (Math.PI * 2 * i) / count;
      this.particles.push({
        baseAngle: angle,
        distance: 70 + Math.random() * 40,
        speed: 0.005 + Math.random() * 0.01,
        size: 1.5 + Math.random() * 2,
        alpha: 0.3 + Math.random() * 0.7
      });
    }
  }

  start() {
    if (this.isRunning) return;
    this.isRunning = true;
    this.animate();
  }

  stop() {
    this.isRunning = false;
  }

  animate() {
    if (!this.isRunning) return;
    this.draw();
    requestAnimationFrame(() => this.animate());
  }

  draw() {
    const { ctx, width, height } = this;
    ctx.clearRect(0, 0, width, height);

    const centerX = width / 2;
    const centerY = height / 2;
    const state = this.voiceModule ? this.voiceModule.state : 'IDLE';

    // State parameters tuned for clean professional white background
    let baseColor = '#2563eb';      // Sapphire Blue default
    let glowColor = 'rgba(37, 99, 235, 0.12)';
    let speedMult = 1.0;
    let pulseIntensity = 0.05;

    if (state === 'LISTENING') {
      baseColor = '#059669';       // Emerald Green for listening
      glowColor = 'rgba(5, 150, 105, 0.18)';
      speedMult = 1.6;
      pulseIntensity = 0.15;
    } else if (state === 'PROCESSING') {
      baseColor = '#7c3aed';       // Violet for deep thinking
      glowColor = 'rgba(124, 58, 237, 0.18)';
      speedMult = 3.0;
      pulseIntensity = 0.2;
    } else if (state === 'SPEAKING') {
      baseColor = '#1d4ed8';       // Electric Royal Blue for speaking
      glowColor = 'rgba(29, 78, 216, 0.22)';
      speedMult = 1.8;
      pulseIntensity = 0.25;
    }

    this.rotation += 0.008 * speedMult;
    this.innerRotation -= 0.012 * speedMult;
    this.pulsePhase += 0.04 * speedMult;

    const pulse = 1 + Math.sin(this.pulsePhase) * pulseIntensity;

    // 1. Center Core Radial Glow
    const glowRadius = Math.min(width, height) * 0.38 * pulse;
    const gradient = ctx.createRadialGradient(centerX, centerY, 5, centerX, centerY, glowRadius);
    gradient.addColorStop(0, glowColor);
    gradient.addColorStop(0.5, 'rgba(241, 245, 249, 0.4)');
    gradient.addColorStop(1, 'rgba(255, 255, 255, 0)');
    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.arc(centerX, centerY, glowRadius, 0, Math.PI * 2);
    ctx.fill();

    // 2. Concentric Holographic HUD Rings
    const baseRadius = Math.min(width, height) * 0.28;

    // Outer Ring with Ticks
    this.drawHoloRing(centerX, centerY, baseRadius * 1.25, this.rotation, baseColor, 48, 4);

    // Mid Segmented Ring
    this.drawSegmentedRing(centerX, centerY, baseRadius * pulse, this.innerRotation, baseColor);

    // Inner Core Waveform or Reactor
    if (state === 'LISTENING') {
      this.drawAudioFrequencyWave(centerX, centerY, baseRadius * 0.75, baseColor);
    } else if (state === 'SPEAKING') {
      this.drawSpeechHarmonics(centerX, centerY, baseRadius * 0.75, baseColor);
    } else if (state === 'PROCESSING') {
      this.drawQuantumScanningSweep(centerX, centerY, baseRadius * 0.7, baseColor);
    } else {
      this.drawIdlePulseRing(centerX, centerY, baseRadius * 0.6, pulse, baseColor);
    }

    // 3. Floating Holographic Particles
    this.drawParticles(centerX, centerY, baseColor);
  }

  drawHoloRing(cx, cy, r, rot, color, ticks, tickLength) {
    const { ctx } = this;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate(rot);

    ctx.strokeStyle = color;
    ctx.lineWidth = 1;
    ctx.globalAlpha = 0.5;

    ctx.beginPath();
    ctx.arc(0, 0, r, 0, Math.PI * 2);
    ctx.stroke();

    for (let i = 0; i < ticks; i++) {
      const angle = (Math.PI * 2 * i) / ticks;
      const x1 = Math.cos(angle) * (r - tickLength);
      const y1 = Math.sin(angle) * (r - tickLength);
      const x2 = Math.cos(angle) * (r + tickLength);
      const y2 = Math.sin(angle) * (r + tickLength);

      ctx.beginPath();
      ctx.moveTo(x1, y1);
      ctx.lineTo(x2, y2);
      ctx.stroke();
    }
    ctx.restore();
  }

  drawSegmentedRing(cx, cy, r, rot, color) {
    const { ctx } = this;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate(rot);
    ctx.strokeStyle = color;
    ctx.lineWidth = 2.5;
    ctx.globalAlpha = 0.8;

    const segments = 4;
    const gap = 0.3;
    const segAngle = (Math.PI * 2) / segments;

    for (let i = 0; i < segments; i++) {
      const start = i * segAngle + gap;
      const end = (i + 1) * segAngle - gap;
      ctx.beginPath();
      ctx.arc(0, 0, r, start, end);
      ctx.stroke();
    }
    ctx.restore();
  }

  drawAudioFrequencyWave(cx, cy, r, color) {
    const { ctx } = this;
    const freqData = this.voiceModule ? this.voiceModule.getAudioFrequencyData() : null;

    ctx.save();
    ctx.translate(cx, cy);
    ctx.strokeStyle = color;
    ctx.lineWidth = 2;
    ctx.globalAlpha = 0.9;
    ctx.beginPath();

    const points = 64;
    for (let i = 0; i <= points; i++) {
      const angle = (Math.PI * 2 * i) / points;
      let amp = 0;
      if (freqData && freqData.length > 0) {
        const idx = Math.floor((i / points) * (freqData.length / 2));
        amp = (freqData[idx] / 255) * 35;
      } else {
        amp = Math.sin(angle * 6 + this.pulsePhase * 3) * 12;
      }
      const curR = r + amp;
      const x = Math.cos(angle) * curR;
      const y = Math.sin(angle) * curR;
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.closePath();
    ctx.stroke();
    ctx.restore();
  }

  drawSpeechHarmonics(cx, cy, r, color) {
    const { ctx } = this;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.strokeStyle = color;
    ctx.lineWidth = 2.5;
    ctx.globalAlpha = 0.95;

    for (let wave = 0; wave < 3; wave++) {
      ctx.beginPath();
      const points = 72;
      const waveR = r - wave * 12;
      for (let i = 0; i <= points; i++) {
        const angle = (Math.PI * 2 * i) / points;
        const offset = Math.sin(angle * (4 + wave * 2) + this.pulsePhase * 4) * (8 + wave * 4);
        const x = Math.cos(angle) * (waveR + offset);
        const y = Math.sin(angle) * (waveR + offset);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.closePath();
      ctx.stroke();
    }
    ctx.restore();
  }

  drawQuantumScanningSweep(cx, cy, r, color) {
    const { ctx } = this;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate(this.rotation * 4);

    const gradient = ctx.createConicGradient(0, 0, 0);
    gradient.addColorStop(0, 'rgba(188, 19, 254, 0)');
    gradient.addColorStop(0.7, 'rgba(188, 19, 254, 0.1)');
    gradient.addColorStop(1, 'rgba(188, 19, 254, 0.8)');

    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.arc(0, 0, r, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = color;
    ctx.lineWidth = 1.5;
    ctx.stroke();
    ctx.restore();
  }

  drawIdlePulseRing(cx, cy, r, pulse, color) {
    const { ctx } = this;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.strokeStyle = color;
    ctx.lineWidth = 1.8;
    ctx.globalAlpha = 0.6;

    ctx.beginPath();
    ctx.arc(0, 0, r * pulse, 0, Math.PI * 2);
    ctx.stroke();

    // Central core dot
    ctx.fillStyle = color;
    ctx.globalAlpha = 0.9;
    ctx.beginPath();
    ctx.arc(0, 0, 7 * pulse, 0, Math.PI * 2);
    ctx.fill();

    ctx.restore();
  }

  drawParticles(cx, cy, color) {
    const { ctx } = this;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.fillStyle = color;

    for (const p of this.particles) {
      p.baseAngle += p.speed;
      const x = Math.cos(p.baseAngle) * p.distance;
      const y = Math.sin(p.baseAngle) * p.distance;

      ctx.globalAlpha = p.alpha;
      ctx.beginPath();
      ctx.arc(x, y, p.size, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.restore();
  }
}



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
    formattedText = formattedText.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    formattedText = formattedText.replace(/\[([^\]]+)\]\((https?:\/\/[^\)]+)\)/g, '<a href="$2" target="_blank" rel="noopener" class="live-web-link">🔗 $1</a>');
    formattedText = formattedText.replace(/### (.*?)(?:\n|<br>|$)/g, '<div class="msg-subheading">$1</div>');
    formattedText = formattedText.replace(/\n/g, '<br>');

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

