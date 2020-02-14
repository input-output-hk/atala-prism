import { getNamesAndSurnames } from '../APIs/credentials/credentialsManager';

it('getNamesAndSurnames', () => {
  const names = ['First', 'Name'];
  const surnames = ['Last', 'Name'];
  expect(getNamesAndSurnames('First@Name Last@Name')).toMatchObject({ names, surnames });
});

it('getNamesAndSurnames surnames is undefined', () => {
  const names = ['First', 'Name'];
  const surnames = [''];
  expect(getNamesAndSurnames('First@Name')).toMatchObject({ names, surnames });
});
