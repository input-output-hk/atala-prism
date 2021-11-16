import * as JsSearch from 'js-search';

export const buildSearchIndex = documents => {
  const dataToSearch = new JsSearch.Search('id');
  dataToSearch.sanitizer = new JsSearch.LowerCaseSanitizer();
  dataToSearch.searchIndex = new JsSearch.UnorderedSearchIndex();
  dataToSearch.indexStrategy = new JsSearch.ExactWordIndexStrategy();
  dataToSearch.addIndex(['frontmatter', 'title']);
  dataToSearch.addIndex(['frontmatter', 'description']);
  dataToSearch.addIndex(['internal', 'content']);
  dataToSearch.addDocuments(documents);
  return dataToSearch;
};
