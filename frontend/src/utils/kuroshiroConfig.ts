import Kuroshiro from 'kuroshiro';
import KuromojiAnalyzer from 'kuroshiro-analyzer-kuromoji';

/**
 * Creates and initializes a Kuroshiro instance with proper browser configuration
 */
export async function createKuroshiro(): Promise<Kuroshiro> {
  try {
    // Create a new Kuroshiro instance
    const kuroshiro = new Kuroshiro();

    // Configure the dictionary path properly for browser environments
    const dictPath = window.location.origin + '/dict';

    // Create an analyzer with the browser-compatible path
    const analyzer = new KuromojiAnalyzer({ dictPath });

    // Initialize with the analyzer
    await kuroshiro.init(analyzer);

    return kuroshiro;
  } catch (error) {

    // Re-throw for the caller to handle
    throw error;
  }
}

/**
 * Simple furigana formatter that uses basic HTML without requiring Kuroshiro
 * This can be used as a fallback when Kuroshiro fails to initialize
 */
export function simpleFurigana(text: string, readings?: Record<string, string>): string {
  if (!text) return '';

  // If no readings provided, just return the original text
  if (!readings) return `<span class="japanese-text">${text}</span>`;

  // Otherwise use the readings to create basic ruby markup
  let result = '';

  // This is a very basic implementation - in a real app you'd need
  // more sophisticated text segmentation and reading assignment
  for (const kanji of Object.keys(readings)) {
    if (text.includes(kanji)) {
      const reading = readings[kanji];
      text = text.replace(
        kanji,
        `<ruby>${kanji}<rt>${reading}</rt></ruby>`
      );
    }
  }

  return `<span class="japanese-text">${text}</span>`;
}