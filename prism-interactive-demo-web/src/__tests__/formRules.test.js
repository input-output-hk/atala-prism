import { futureDate, noEmptyInput } from '../app/helpers/formRules';

describe('FormRules', () => {
  const expectSuccess = res => expect(res).toBeFalsy();
  const expectError = res => expect(res).toBe('error');

  describe('futureDate', () => {
    it('should return nothing when is future date', () => {
      futureDate('2021-12-13', expectSuccess, '2021-12-14');
    });

    it('should return an error message when is past date', () => {
      futureDate('2021-12-13', expectError, '2021-12-12');
    });
  });

  describe('noEmptyInput', () => {
    it('should return expected value', () => {
      const message = 'test message';
      const res = noEmptyInput(message);

      expect(res.message).toBe(message);
      expect(res.required).toBeTruthy();
      expect(res.whitespace).toBeTruthy();
    });
  });
});
