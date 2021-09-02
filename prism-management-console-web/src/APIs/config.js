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
  saveTutorialProgress
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
