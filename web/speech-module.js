/**
 * Karllo AI - JARVIS-Style Voice Interaction Module
 * ===================================================
 * Powered by Web Speech API:
 * - SpeechRecognition / webkitSpeechRecognition for real-time speech-to-text
 * - window.speechSynthesis for natural voice responses
 * - Web Audio API AnalyserNode for reactive audio waveforms
 */

export class KarlloVoiceModule {
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

    // Native Android Bridge listener
    if (typeof window !== 'undefined') {
      window.onNativeSpeechResult = (text) => {
        if (text) {
          this.onTranscript(text, true);
        }
        this.setState('IDLE');
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
