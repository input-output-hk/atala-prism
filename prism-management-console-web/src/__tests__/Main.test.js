import React from 'react';
import ReactDOM from 'react-dom';
import { BrowserRouter } from 'react-router-dom';
import { act } from '@testing-library/react';
import { mockApi } from '../APIs/__mocks__';
import { isDevEnv } from '../APIs/env';
import { APIContext } from '../components/providers/ApiContext';
import App from '../App';
import { createStores, GlobalStateContext } from '../stores';

test('act works in this case', async () => {
  const stores = createStores(mockApi);
  const div = document.createElement('div');
  await act(async () => {
    ReactDOM.render(
      <APIContext.Provider value={mockApi}>
        <GlobalStateContext.Provider value={stores}>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </GlobalStateContext.Provider>
      </APIContext.Provider>,
      div
    );
    ReactDOM.unmountComponentAtNode(div);
  });
});

it('isDevEnv correctly spots development and local environments', () => {
  expect(isDevEnv('http://cvp-develop.cef.iohkdev.io:8080')).toEqual(true);
  expect(isDevEnv('https://cvp-develop.cef.iohkdev.io:8080')).toEqual(true);
  expect(isDevEnv('http://localhost:8080')).toEqual(true);
  expect(isDevEnv('https://localhost:8080')).toEqual(true);
  expect(isDevEnv('https://demo.atala-prism.io:8080')).toEqual(false);
});
