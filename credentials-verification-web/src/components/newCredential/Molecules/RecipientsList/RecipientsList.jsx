import React from 'react';
import PropTypes from 'prop-types';
import { Table } from 'antd';
import contactLogo from '../../../../images/holder-default-avatar.svg';
import groupLogo from '../../../../images/groupIcon.svg';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import { dayMonthYearBackendFormatter } from '../../../../helpers/formatters';

import './_style.scss';
import { useTranslationWithPrefix } from '../../../../hooks/useTranslationWithPrefix';

const contactsPrefix = 'contacts.table.columns';
const groupsPrefix = 'groups.table.columns';

const RecipientsList = ({ recipients }) => {
  const tpc = useTranslationWithPrefix(contactsPrefix);
  const tpg = useTranslationWithPrefix(groupsPrefix);

  return (
    <div className="RecipientsList">
      <Table
        showHeader={false}
        pagination={false}
        columns={[
          {
            width: '60px',
            render: ({ contactid }) => (
              <img
                style={{ width: '40px', height: '40px' }}
                src={contactid ? contactLogo : groupLogo}
                alt="logo"
              />
            )
          },
          {
            render: ({ contactName, groupName }) => (
              <CellRenderer
                title={contactName ? tpc('contactName') : tpg('groupName')}
                value={contactName || groupName}
              />
            )
          },
          {
            render: ({ externalid }) =>
              externalid && <CellRenderer title={tpc('externalid')} value={externalid} />
          },
          {
            render: ({ creationDate }) =>
              creationDate && (
                <CellRenderer
                  title={tpc('creationDate')}
                  value={dayMonthYearBackendFormatter(creationDate)}
                />
              )
          }
        ]}
        dataSource={recipients}
      />
    </div>
  );
};

RecipientsList.propTypes = {
  recipients: PropTypes.arrayOf(
    PropTypes.oneOf([
      PropTypes.shape({ name: PropTypes.string }),
      PropTypes.shape({
        contactid: PropTypes.string,
        contactName: PropTypes.string,
        externalid: PropTypes.string,
        creationDate: PropTypes.shape({
          day: PropTypes.number,
          month: PropTypes.number,
          year: PropTypes.number
        })
      })
    ])
  ).isRequired
};

export default RecipientsList;
