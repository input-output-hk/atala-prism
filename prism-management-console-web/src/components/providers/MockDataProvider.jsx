import React, { useReducer } from 'react';

const MockDataContext = React.createContext();

const ADD_MOCK_CREDENTIAL_TEMPLATE = 'ADD_MOCK_CREDENTIAL_TEMPLATE';

const mockDataReducer = (mockData, { type, payload }) => {
  const { credentialTypes } = mockData;
  switch (type) {
    case ADD_MOCK_CREDENTIAL_TEMPLATE:
      return {
        ...mockData,
        credentialTypes: credentialTypes.concat(payload.newTemplate)
      };
    default:
      return mockData;
  }
};

const defaultMockData = {
  credentialTypes: []
};

const MockDataProvider = props => {
  const [mockData, mockDataDispatch] = useReducer(mockDataReducer, defaultMockData);

  return <MockDataContext.Provider value={{ mockData, mockDataDispatch }} {...props} />;
};

const useMockDataContext = () => React.useContext(MockDataContext);

const withMockDataProvider = Component => props => (
  <MockDataProvider>
    <Component {...props} />
  </MockDataProvider>
);

export { MockDataProvider, useMockDataContext, withMockDataProvider };
