import React from 'react';
import PropTypes from 'prop-types';
import './_style.scss';
import { Divider } from 'antd';
import { useTranslationWithPrefix } from '../../../hooks/useTranslationWithPrefix';
import imgCard from '../../../images/credentialIcon.svg';
import IconItems from '../Molecules/DashboardCardsItems/IconItems';
import iconSigned from '../../../images/iconSigned.svg';
import iconDraft from '../../../images/iconDraft.svg';
import iconReceived from '../../../images/iconArrowLeft.svg';

const DashboardCardCredential = ({ loading, data }) => {
  const tp = useTranslationWithPrefix('dashboard.analytics');

  return (
    <div className="DashboardContactCardContainer">
      <div className="HeaderCardContainer">
        <div>
          <img src={imgCard} alt="img" srcSet="" />
        </div>
        <span className="titleCardContainer">{tp('credentials')}</span>
      </div>
      <div className="divider">
        <Divider />
      </div>
      <IconItems
        icon={iconDraft}
        title={tp('draft')}
        value={data.numberofcredentialsindraft}
        loading={loading}
      />
      <IconItems
        icon={iconSigned}
        title={tp('signed')}
        value={data.numberofcredentialspublished}
        loading={loading}
      />
      <IconItems
        icon={iconReceived}
        title={tp('received')}
        value={data.numberofcredentialsreceived}
        loading={loading}
      />
    </div>
  );
};

DashboardCardCredential.defaultProps = {
  loading: false,
  data: {
    numberofcredentialsindraft: 0,
    numberofcredentialspublished: 0,
    numberofcredentialsreceived: 0
  }
};

DashboardCardCredential.propTypes = {
  loading: PropTypes.bool,
  data: PropTypes.shape({
    numberofcredentialsindraft: PropTypes.number,
    numberofcredentialspublished: PropTypes.number,
    numberofcredentialsreceived: PropTypes.number
  })
};

export default DashboardCardCredential;
