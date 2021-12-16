import { autoReplacePlaceholders } from '../helpers/credentialView';
import { testTemplates } from './__mocks__/mockCredentialTemplates';

describe('testing credential creation helper functions', () => {
  test('replacing {{placeholders}} in template with credential data works', () => {
    testTemplates.forEach(t => {
      expect(autoReplacePlaceholders(t.rawTemplate, t.credentialData)).toEqual(
        t.expectedAfterReplace
      );
    });
  });
});
