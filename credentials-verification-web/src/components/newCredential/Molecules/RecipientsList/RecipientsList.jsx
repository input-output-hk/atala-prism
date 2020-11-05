import React from 'react';
import PropTypes from 'prop-types';
import { Table } from 'antd';
import contactLogo from '../../../../images/holder-default-avatar.svg';
import groupLogo from '../../../../images/groupIcon.svg';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import { dayMonthYearBackendFormatter } from '../../../../helpers/formatters';

import './_style.scss';

const RecipientsList = ({ recipients }) => (
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
          render: ({ contactName, name }) => (
            <CellRenderer
              title={contactName ? 'contactName' : 'groupName'}
              value={contactName || name}
              componentName={contactName ? 'contacts' : 'groups'}
            />
          )
        },
        {
          render: ({ externalid }) =>
            externalid && (
              <CellRenderer title="externalid" value={externalid} componentName="contacts" />
            )
        },
        {
          render: ({ creationDate }) =>
            creationDate && (
              <CellRenderer
                title="creationDate"
                value={dayMonthYearBackendFormatter(creationDate)}
                componentName="contacts"
              />
            )
        }
      ]}
      dataSource={recipients}
    />
  </div>
);

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
