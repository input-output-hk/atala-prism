import React from 'react';
import ReactDOM from 'react-dom';
import { BrowserRouter } from 'react-router-dom';
import { act } from '@testing-library/react';
import Main from '../components/main/Main';
import { mockApi } from '../APIs/__mocks__';

test('act works in this case', async () => {
  const div = document.createElement('div');
  await act(async () => {
    ReactDOM.render(
      <BrowserRouter>
        <Main apiProvider={{ ...mockApi }} />
      </BrowserRouter>,
      div
    );
    ReactDOM.unmountComponentAtNode(div);
  });
});
