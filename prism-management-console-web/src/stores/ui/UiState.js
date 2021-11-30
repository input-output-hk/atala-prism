import TemplateUiState from './TemplateUiState';
import TemplateSketchState from './TemplateSketchState';

export class UiState {
  constructor(api, rootStore) {
    this.templateUiState = new TemplateUiState(rootStore);
    this.templateSketchState = new TemplateSketchState(rootStore);
  }
}
