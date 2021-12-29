import React from 'react';
import ReactDOM from 'react-dom';
import { BrowserRouter } from 'react-router-dom';
import { act } from '@testing-library/react';
import { mockApi } from '../APIs/__mocks__';
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
