import { getTextToHighlight, isFirstWord, removeHtmlTags } from '../helpers/textFormatter';

const text =
  'Though some controversy remains to this day about whether or not Bell misappropriated such credit from electrical engineer Elisha Gray, who had also been working on developing a remote voice communication device for the past two years, several courts ultimately upheld Bell’s patent claim, which was filed on March 7, 1876. (Gray did not contend this, as he accepted that his prototype and Bell’s invention differed in some fundamental ways.)';

const noPrefixMatch = `${text.slice(0, 400)}...`;
const prefixMatch = `...${text.slice(2, 402)}...`;
const noSufixMatch = `...${text.slice(44)}`;

describe('Text formatter', () => {
  describe('Remove HTML tags', () => {
    it('Should remove arbitrary HTML tags', () => {
      const html = '<html><body><a>Some</a><p> HTML </p><h1>content</h1><img /></body></html>';
      const result = removeHtmlTags(html);
      expect(result).toBe('Some HTML content');
    });

    it('Should not modify string with no HTML tags', () => {
      const testString = 'Some string';
      const result = removeHtmlTags(testString);
      expect(result).toBe(testString);
    });
  });

  describe('Is first word', () => {
    it('Should return true when text starts with provided word', () => {
      const result = isFirstWord(text, 'Though');
      expect(result).toBe(true);
    });

    it('Should return false when text does not start with provided word', () => {
      const result = isFirstWord(text, 'from');
      expect(result).toBe(false);
    });
  });

  describe('Get text to highlight', () => {
    it('Should return text starting with provided word and ending with "..." when it matches the first word', () => {
      const result = getTextToHighlight(text, 'Though');
      expect(result).toBe(noPrefixMatch);
    });

    it('Should return text starting with provided word and ending with "..." when it matches a word starting in the first 100 characters', () => {
      const result = getTextToHighlight(text, 'from');
      expect(result).toBe(noPrefixMatch);
    });

    it('Should return text starting with "..." and ending with "..." when provided word matches a word after the first 100 and before the last 300 characters', () => {
      const result = getTextToHighlight(text, 'electrical');
      expect(result).toBe(prefixMatch);
    });

    it('Should return text starting with "..." and ending without "..." when provided word matches a word in the last 300 characters', () => {
      const result = getTextToHighlight(text, 'also');
      expect(result).toBe(noSufixMatch);
    });
  });
});
