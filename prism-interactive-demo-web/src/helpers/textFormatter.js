const PREFIX_LENGTH = 100;
const DISPLAY_LENGTH = 400;

export const removeHtmlTags = html => html.replace(/(<([^>]+)>)/gi, '');

export const isFirstWord = (text, word) => text.substring(0, word.length + 1) === `${word} `;

export const getTextToHighlight = (html, query) => {
  const lowerCaseQuery = query.toLowerCase();

  const postAsText = removeHtmlTags(html);
  const postAsLowerCaseText = postAsText.toLowerCase();

  const queryIsFirstWord = isFirstWord(postAsLowerCaseText, lowerCaseQuery);
  const firstInstanceIndex = queryIsFirstWord
    ? 0
    : postAsLowerCaseText.indexOf(` ${lowerCaseQuery} `);

  const startIndex = Math.max(0, firstInstanceIndex - PREFIX_LENGTH);
  const endIndex = startIndex + DISPLAY_LENGTH;

  const prefix = startIndex ? '...' : '';
  const content = postAsText.substring(startIndex, endIndex).trim();
  const sufix = endIndex >= postAsText.length ? '' : '...';

  return `${prefix}${content}${sufix}`;
};
