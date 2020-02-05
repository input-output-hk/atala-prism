import React from 'react';
import ReactDOM from 'react-dom';
import { BrowserRouter } from 'react-router-dom';
import Main from '../components/main/Main';
import { mockApi } from '../APIs/__mocks__';
import { isDevEnv } from '../APIs/env';

it('renders without crashing', () => {
  const div = document.createElement('div');
  ReactDOM.render(
    <BrowserRouter>
      <Main apiProvider={{ ...mockApi }} />
    </BrowserRouter>,
    div
  );
  ReactDOM.unmountComponentAtNode(div);
});

it('isDevEnv correctly spots development and local environments', () => {
  expect(isDevEnv('http://cvp-develop.cef.iohkdev.io:8080')).toEqual(true);
  expect(isDevEnv('https://cvp-develop.cef.iohkdev.io:8080')).toEqual(true);
  expect(isDevEnv('http://localhost:8080')).toEqual(true);
  expect(isDevEnv('https://localhost:8080')).toEqual(true);
  expect(isDevEnv('https://demo.atala-prism.io:8080')).toEqual(false);
});
