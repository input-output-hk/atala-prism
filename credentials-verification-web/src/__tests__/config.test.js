import { config } from '../APIs/config';

it('Should save create a new configuration', () => {
  config.userRole.remove();
  expect(config.userRole.get()).toBeNull();
  const userRole = 'role';
  config.userRole.set(userRole);
  expect(config.userRole.get()).toBe(userRole);
  config.userRole.remove();
  expect(config.userRole.get()).toBeNull();
});
