import React, { useState, useEffect } from 'react';
import Landing from '../landing/Landing';
import Dashboard from '../dashboard/Dashboard';
import Loading from '../common/Atoms/Loading/Loading';
import { withSideBar } from './withSideBar';
import { withApi } from './withApi';
import { USER_ROLE, UNLOCKED } from '../../helpers/constants';

const withLoggedValidationComponent = (Component, validRoles) => props => {
  const DashboardWithSidebar = withSideBar(Dashboard);
  const {
    api: { wallet }
  } = props;

  const [loading, setLoading] = useState(true);
  const [walletSessionIsValid, setWalletSessionIsValid] = useState();

  useEffect(() => {
    const isWalletRequired = !!validRoles.length;

    const fetchSession = async () => {
      const session = await wallet.getSession({ timeout: 5000 });
      const sessionIsValid = session?.sessionState === UNLOCKED;
      setLoading(false);
      setWalletSessionIsValid(sessionIsValid);
    };

    if (isWalletRequired) fetchSession();
    else setLoading(false);
  }, []);

  if (loading) return <Loading />;

  // Here is where the validations are made
  const role = localStorage.getItem(USER_ROLE);
  const userHasRole = !!role;

  const canRender = () => {
    // The wallet is required to be unlocked only if any role is required for
    // the route.
    const isWalletRequired = !!validRoles.length;

    // If the route is public, aka does not require the wallet being unlocked,
    // it needs only the wallet to be locked or no role to be in the local
    // storage to show it, since it means no user is logged.
    const canRenderPublic = !(isWalletRequired || (walletSessionIsValid && userHasRole));

    // The role is valid only if the current role is in the array passed to the
    // provider.
    const isValidRole = validRoles.includes(role);
    return canRenderPublic || (walletSessionIsValid && isValidRole);
  };

  // If is either public and no user is logger or meets the requeriments, the
  // wanted component is returned.
  // if (canRender()) return <Component {...props} />;
  if (canRender()) return <Component {...props} />;

  // If the previous validation turns out to fail, because it is not public and
  // the wallet is locked or the role is not one with access to this route, the
  // walled status is checked. If the wallet is locked or there is no rolethen
  // the validation failed because someone not logged tried to enter a private
  // route.
  if (!walletSessionIsValid || !userHasRole) return <Landing />;

  // Otherwise it is because the logged user has a role that has no access to
  // that route and therefore is redirected to the dashboard.
  return <DashboardWithSidebar />;
};

export const withLoggedValidation = (Component, validRoles) =>
  withApi(withLoggedValidationComponent(Component, validRoles));
