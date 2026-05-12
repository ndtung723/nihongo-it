import type Kuroshiro from "kuroshiro";

// Dynamic import: kuroshiro + kuromoji dictionary (~12MB) loads only when called
export async function createKuroshiro(): Promise<Kuroshiro> {
  try {
    const { default: KuroshiroClass } = await import("kuroshiro");
    const { default: KuromojiAnalyzer } = await import(
      "kuroshiro-analyzer-kuromoji"
    );

    const kuroshiro = new KuroshiroClass();
    const dictPath = window.location.origin + "/dict";
    const analyzer = new KuromojiAnalyzer({ dictPath });
    await kuroshiro.init(analyzer);
    return kuroshiro;
  } catch (error) {
    throw error;
  }
}

export function simpleFurigana(
  text: string,
  readings?: Record<string, string>,
): string {
  if (!text) return "";
  if (!readings) return `<span class="japanese-text">${text}</span>`;

  for (const kanji of Object.keys(readings)) {
    if (text.includes(kanji)) {
      const reading = readings[kanji];
      text = text.replace(kanji, `<ruby>${kanji}<rt>${reading}</rt></ruby>`);
    }
  }

  return `<span class="japanese-text">${text}</span>`;
}
