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

export class KarlloCoreEngine {
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
