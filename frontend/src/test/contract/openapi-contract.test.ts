/**
 * Contract tests: verify that TypeScript interfaces in types.ts match the OpenAPI spec.
 * Parses the flattened api-definition.yaml and checks field names, required fields,
 * and enum values against our manual type definitions.
 */
import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';
import yaml from 'js-yaml';

interface OpenApiProperty {
  type?: string;
  format?: string;
  enum?: string[];
  $ref?: string;
  items?: OpenApiProperty;
  nullable?: boolean;
}

interface OpenApiSchema {
  type?: string;
  required?: string[];
  properties?: Record<string, OpenApiProperty>;
  enum?: string[];
  allOf?: (OpenApiSchema | { $ref: string })[];
}

interface OpenApiSpec {
  components: {
    schemas: Record<string, OpenApiSchema>;
  };
  paths: Record<string, Record<string, {
    operationId?: string;
    requestBody?: {
      content: Record<string, { schema: OpenApiProperty }>;
    };
    responses?: Record<string, {
      content?: Record<string, { schema: OpenApiProperty }>;
    }>;
  }>>;
}

const specPath = resolve(__dirname, '../../../../api/src/main/resources/api-definition.yaml');
const spec = yaml.load(readFileSync(specPath, 'utf-8')) as OpenApiSpec;
const schemas = spec.components.schemas;

function resolveSchema(schema: OpenApiSchema): OpenApiSchema {
  if (!schema.allOf) return schema;
  const merged: OpenApiSchema = { type: 'object', properties: {}, required: [] };
  for (const part of schema.allOf) {
    const resolved = '$ref' in part
      ? schemas[((part as { $ref: string }).$ref as string).replace('#/components/schemas/', '')]
      : part as OpenApiSchema;
    if (!resolved) continue;
    const inner = resolveSchema(resolved);
    Object.assign(merged.properties!, inner.properties ?? {});
    merged.required = [...(merged.required ?? []), ...(inner.required ?? [])];
  }
  return merged;
}

function getSchemaFields(schemaName: string): string[] {
  const schema = schemas[schemaName];
  if (!schema) return [];
  const resolved = resolveSchema(schema);
  return Object.keys(resolved.properties ?? {});
}

function getRequiredFields(schemaName: string): string[] {
  const schema = schemas[schemaName];
  if (!schema) return [];
  return resolveSchema(schema).required ?? [];
}

describe('OpenAPI contract: response schemas', () => {
  it('ProjectResponse has correct fields', () => {
    const fields = getSchemaFields('ProjectResponse');
    expect(fields).toContain('id');
    expect(fields).toContain('name');
    expect(fields).toContain('slug');
    expect(fields).toContain('createdAt');
    expect(fields).not.toContain('adminCode');
    expect(fields).not.toContain('rooms');
  });

  it('ProjectAdminResponse has adminCode field', () => {
    const fields = getSchemaFields('ProjectAdminResponse');
    expect(fields).toContain('id');
    expect(fields).toContain('name');
    expect(fields).toContain('slug');
    expect(fields).toContain('adminCode');
    expect(fields).toContain('createdAt');
  });

  it('RoomResponse has correct fields', () => {
    const fields = getSchemaFields('RoomResponse');
    expect(fields).toContain('id');
    expect(fields).toContain('projectId');
    expect(fields).toContain('title');
    expect(fields).toContain('roomType');
    expect(fields).toContain('deadline');
    expect(fields).toContain('status');
    expect(fields).toContain('createdAt');
    expect(fields).not.toContain('taskCount');
  });

  it('RoomDetailResponse has tasks array', () => {
    const fields = getSchemaFields('RoomDetailResponse');
    expect(fields).toContain('id');
    expect(fields).toContain('title');
    expect(fields).toContain('roomType');
    expect(fields).toContain('status');
    expect(fields).toContain('tasks');
    expect(fields).not.toContain('progress');
  });

  it('RoomAdminResponse has tasks and participants', () => {
    const fields = getSchemaFields('RoomAdminResponse');
    expect(fields).toContain('tasks');
    expect(fields).toContain('participants');
  });

  it('RoomProgressResponse has task-level progress', () => {
    const fields = getSchemaFields('RoomProgressResponse');
    expect(fields).toContain('roomId');
    expect(fields).toContain('status');
    expect(fields).toContain('totalParticipants');
    expect(fields).toContain('tasks');
  });

  it('RoomResultsResponse has correct fields', () => {
    const fields = getSchemaFields('RoomResultsResponse');
    expect(fields).toContain('roomId');
    expect(fields).toContain('title');
    expect(fields).toContain('status');
    expect(fields).toContain('tasks');
    expect(fields).not.toContain('participants');
  });

  it('TaskWithEstimateResponse has myEstimate and votedCount', () => {
    const fields = getSchemaFields('TaskWithEstimateResponse');
    expect(fields).toContain('id');
    expect(fields).toContain('title');
    expect(fields).toContain('sortOrder');
    expect(fields).toContain('myEstimate');
    expect(fields).toContain('allEstimates');
    expect(fields).toContain('averagePoints');
    expect(fields).toContain('medianPoints');
    expect(fields).toContain('finalEstimate');
    expect(fields).toContain('votedCount');
    expect(fields).toContain('totalParticipants');
    expect(fields).not.toContain('position');
  });

  it('TaskWithAllEstimatesResponse has estimates array and finalEstimate', () => {
    const fields = getSchemaFields('TaskWithAllEstimatesResponse');
    expect(fields).toContain('id');
    expect(fields).toContain('title');
    expect(fields).toContain('estimates');
    expect(fields).toContain('finalEstimate');
    expect(fields).not.toContain('taskId');
    expect(fields).not.toContain('taskTitle');
  });

  it('TaskResultResponse uses averagePoints, medianPoints, and finalEstimate', () => {
    const fields = getSchemaFields('TaskResultResponse');
    expect(fields).toContain('taskId');
    expect(fields).toContain('title');
    expect(fields).toContain('estimates');
    expect(fields).toContain('averagePoints');
    expect(fields).toContain('medianPoints');
    expect(fields).toContain('finalEstimate');
    expect(fields).not.toContain('average');
    expect(fields).not.toContain('median');
  });

  it('TaskProgressResponse has votedCount and totalParticipants', () => {
    const fields = getSchemaFields('TaskProgressResponse');
    expect(fields).toContain('taskId');
    expect(fields).toContain('title');
    expect(fields).toContain('votedCount');
    expect(fields).toContain('totalParticipants');
  });

  it('EstimateResponse uses participantNickname and storyPoints', () => {
    const fields = getSchemaFields('EstimateResponse');
    expect(fields).toContain('id');
    expect(fields).toContain('participantId');
    expect(fields).toContain('participantNickname');
    expect(fields).toContain('storyPoints');
    expect(fields).toContain('createdAt');
    expect(fields).not.toContain('nickname');
    expect(fields).not.toContain('value');
  });

  it('ParticipantResponse has correct fields including token', () => {
    const fields = getSchemaFields('ParticipantResponse');
    expect(fields).toContain('id');
    expect(fields).toContain('nickname');
    expect(fields).toContain('invitedRoomIds');
    expect(fields).toContain('createdAt');
    expect(fields).toContain('token');
    expect(fields).toContain('tokenExpiresAt');
    expect(fields).not.toContain('role');
  });

  it('ProjectAdminResponse has token and tokenExpiresAt fields', () => {
    const fields = getSchemaFields('ProjectAdminResponse');
    expect(fields).toContain('token');
    expect(fields).toContain('tokenExpiresAt');
  });

  it('GuestTokenResponse has token and expiresAt fields', () => {
    const fields = getSchemaFields('GuestTokenResponse');
    expect(fields).toContain('token');
    expect(fields).toContain('expiresAt');
    const required = getRequiredFields('GuestTokenResponse');
    expect(required).toContain('token');
    expect(required).toContain('expiresAt');
  });
});

describe('OpenAPI contract: request schemas', () => {
  it('CreateProjectRequest requires name', () => {
    const required = getRequiredFields('CreateProjectRequest');
    const fields = getSchemaFields('CreateProjectRequest');
    expect(required).toContain('name');
    expect(fields).toContain('name');
    expect(fields).not.toContain('isPublic');
  });

  it('CreateRoomRequest requires roomType and title', () => {
    const required = getRequiredFields('CreateRoomRequest');
    expect(required).toContain('roomType');
    expect(required).toContain('title');
  });

  it('SubmitEstimateRequest uses storyPoints (not value)', () => {
    const fields = getSchemaFields('SubmitEstimateRequest');
    expect(fields).toContain('storyPoints');
    expect(fields).not.toContain('value');
    // storyPoints is optional (nullable) - allows saving comment draft without SP
    const required = getRequiredFields('SubmitEstimateRequest');
    expect(required).not.toContain('storyPoints');
  });

  it('ImportTasksRequest requires titles array', () => {
    const fields = getSchemaFields('ImportTasksRequest');
    expect(fields).toContain('titles');
    const required = getRequiredFields('ImportTasksRequest');
    expect(required).toContain('titles');
  });

  it('JoinProjectRequest requires nickname and has optional roomId', () => {
    const fields = getSchemaFields('JoinProjectRequest');
    expect(fields).toContain('nickname');
    expect(fields).toContain('roomId');
    const required = getRequiredFields('JoinProjectRequest');
    expect(required).toContain('nickname');
    expect(required).not.toContain('roomId');
  });

  it('SetFinalEstimateRequest requires storyPoints', () => {
    const fields = getSchemaFields('SetFinalEstimateRequest');
    expect(fields).toContain('storyPoints');
    const required = getRequiredFields('SetFinalEstimateRequest');
    expect(required).toContain('storyPoints');
  });
});

describe('OpenAPI contract: enums', () => {
  it('StoryPoints enum has correct values', () => {
    const schema = schemas['StoryPoints'];
    expect(schema?.enum).toEqual(['0', '0.5', '1', '2', '3', '5', '8', '13', '21', '?', 'N/A']);
    expect(schema?.type).toBe('string');
  });

  it('RoomStatus enum has correct values', () => {
    const schema = schemas['RoomStatus'];
    expect(schema?.enum).toEqual(['OPEN', 'REVEALED', 'CLOSED']);
  });

  it('RoomType enum has correct values', () => {
    const schema = schemas['RoomType'];
    expect(schema?.enum).toEqual(['ASYNC', 'LIVE']);
  });
});

describe('OpenAPI contract: API paths exist', () => {
  const paths = Object.keys(spec.paths);

  it('project endpoints exist', () => {
    expect(paths).toContain('/projects');
    expect(paths).toContain('/projects/{slug}');
    expect(paths).toContain('/projects/{slug}/admin');
    expect(paths).toContain('/projects/{slug}/rooms');
    expect(paths).toContain('/projects/{slug}/participants');
  });

  it('room endpoints use flat paths (not nested under projects)', () => {
    expect(paths).toContain('/rooms/{roomId}');
    expect(paths).toContain('/rooms/{roomId}/admin');
    expect(paths).toContain('/rooms/{roomId}/reveal');
    expect(paths).toContain('/rooms/{roomId}/progress');
    expect(paths).toContain('/rooms/{roomId}/results');
    expect(paths).toContain('/rooms/{roomId}/results/export');
    expect(paths).toContain('/rooms/{roomId}/tasks');
    expect(paths).toContain('/rooms/{roomId}/tasks/import');
  });

  it('task/estimate endpoints use flat paths', () => {
    expect(paths).toContain('/tasks/{taskId}');
    expect(paths).toContain('/tasks/{taskId}/estimates');
    expect(paths).toContain('/tasks/{taskId}/final-estimate');
  });

  it('no nested project/room/task paths exist (old wrong pattern)', () => {
    const nested = paths.filter((p) =>
      p.match(/\/projects\/\{slug\}\/rooms\/\{roomId\}/)
    );
    expect(nested).toEqual([]);
  });
});

describe('OpenAPI quality rules', () => {
  it('all response schemas should declare required fields', () => {
    const responseSchemaNames = Object.keys(schemas).filter(
      (name) =>
        name.endsWith('Response') ||
        name.endsWith('Entry') ||
        name.endsWith('Status') ||
        name === 'RoundHistoryVote'
    );
    const missing: string[] = [];
    for (const name of responseSchemaNames) {
      const schema = schemas[name];
      if (!schema) continue;
      const resolved = resolveSchema(schema);
      // Skip pure enum/primitive schemas (no properties to declare required for)
      if (!resolved.properties || Object.keys(resolved.properties).length === 0) continue;
      if (!resolved.required || resolved.required.length === 0) {
        missing.push(name);
      }
    }
    expect(missing, `Schemas missing required array: ${missing.join(', ')}`).toEqual([]);
  });

  it('schemas should not define inline enums (use $ref to enums/)', () => {
    const violations: string[] = [];
    for (const [name, schema] of Object.entries(schemas)) {
      const resolved = resolveSchema(schema);
      for (const [field, prop] of Object.entries(resolved.properties ?? {})) {
        if (prop.enum && !prop.$ref) {
          violations.push(`${name}.${field}`);
        }
      }
    }
    expect(violations, `Inline enums found: ${violations.join(', ')}`).toEqual([]);
  });

  it('endpoints with request body should have 400 response', () => {
    const violations: string[] = [];
    for (const [path, methods] of Object.entries(spec.paths)) {
      for (const [method, operation] of Object.entries(methods)) {
        if (
          ['post', 'patch', 'put'].includes(method) &&
          operation.requestBody &&
          !operation.responses?.['400']
        ) {
          violations.push(`${method.toUpperCase()} ${path}`);
        }
      }
    }
    expect(violations, `Missing 400: ${violations.join(', ')}`).toEqual([]);
  });

  it('endpoints with ID path params should have 404 response', () => {
    const violations: string[] = [];
    for (const [path, methods] of Object.entries(spec.paths)) {
      const hasIdParam = /\{(roomId|taskId|id|participantId)\}/.test(path);
      if (!hasIdParam) continue;
      for (const [method, operation] of Object.entries(methods)) {
        if (
          ['get', 'patch', 'put', 'delete'].includes(method) &&
          !operation.responses?.['404']
        ) {
          violations.push(`${method.toUpperCase()} ${path}`);
        }
      }
    }
    expect(violations, `Missing 404: ${violations.join(', ')}`).toEqual([]);
  });

  it('error responses should have content body', () => {
    const violations: string[] = [];
    for (const [path, methods] of Object.entries(spec.paths)) {
      for (const [method, operation] of Object.entries(methods)) {
        for (const [code, response] of Object.entries(operation.responses ?? {})) {
          if (
            ['400', '401', '403', '404', '409'].includes(code) &&
            !response.content
          ) {
            violations.push(`${method.toUpperCase()} ${path} ${code}`);
          }
        }
      }
    }
    expect(violations, `Empty error bodies: ${violations.join(', ')}`).toEqual([]);
  });
});
