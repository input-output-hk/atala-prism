import { useContext } from 'react';
import { APIContext } from '../components/providers/ApiContext';

export const useApi = () => {
  const api = useContext(APIContext);

  return api;
};
