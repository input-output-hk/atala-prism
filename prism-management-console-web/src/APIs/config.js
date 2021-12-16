import hardcodedTemplateCategories from './credentials/mocks/hardcodedTemplateCategories';

const { REACT_APP_GRPC_CLIENT } = window._env_;

const INITIAL_TUTORIAL_PROGRESS = {
  started: false,
  paused: false,
  finished: false,
  basicSteps: 0,
  contacts: 0,
  groups: 0,
  credentials: 0
};

export const config = {
  grpcClient: getFromLocalStorage('backendUrl') || REACT_APP_GRPC_CLIENT,
  tutorialProgress: getTutorialProgress(),
  saveTutorialProgress,
  getMockedTemplateCategories,
  saveMockedTemplateCategories,
  getCredentialTypesWithCategories,
  saveCredentialTypeWithCategory
};

function getFromLocalStorage(key) {
  return window.localStorage.getItem(key);
}

function getTutorialProgress() {
  const tutorialProgress = getFromLocalStorage('tutorialProgress');
  if (tutorialProgress) return JSON.parse(tutorialProgress);
  return INITIAL_TUTORIAL_PROGRESS;
}

function saveTutorialProgress(tutorialProgress) {
  window.localStorage.setItem('tutorialProgress', JSON.stringify(tutorialProgress));
}

function getMockedTemplateCategories() {
  const templateCategories = getFromLocalStorage('templateCategories');
  if (templateCategories) return JSON.parse(templateCategories);
  return hardcodedTemplateCategories;
}

function saveMockedTemplateCategories(templateCategories) {
  window.localStorage.setItem('templateCategories', JSON.stringify(templateCategories));
}

function getCredentialTypesWithCategories() {
  const templateCategories = getFromLocalStorage('credentialTypeWithCategories');
  if (templateCategories) return JSON.parse(templateCategories);
  return {};
}

function saveCredentialTypeWithCategory({ credentialTypeId, category }) {
  const currentCredentialTypesWithCategories = this.getCredentialTypesWithCategories();
  const updatedCredentialTypesWithCategories = {
    ...currentCredentialTypesWithCategories,
    [credentialTypeId]: category
  };
  window.localStorage.setItem(
    'credentialTypeWithCategories',
    JSON.stringify(updatedCredentialTypesWithCategories)
  );
}
