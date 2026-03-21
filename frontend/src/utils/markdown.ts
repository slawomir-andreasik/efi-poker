import { getTaskSp, type TaskEstimate } from '@/components/ResultsTable';

function escapeMarkdown(text: string): string {
  return text.replace(/\|/g, '\\|');
}

function isConsensus(task: TaskEstimate): boolean {
  const nums = Object.values(task.estimates).filter((v): v is number => typeof v === 'number');
  return nums.length > 0 && new Set(nums).size === 1;
}

function renderTask(task: TaskEstimate, participants: string[], lines: string[]): void {
  lines.push(`### ${task.taskTitle}`);

  for (const name of participants) {
    const sp = task.estimates[name];
    const comment = task.comments[name];

    if (sp == null) {
      lines.push(`- ${name}: -`);
    } else if (comment) {
      lines.push(`- ${name}: **${sp}** - ${escapeMarkdown(comment)}`);
    } else {
      lines.push(`- ${name}: **${sp}**`);
    }
  }

  lines.push('');

  const parts: string[] = [];
  if (task.average != null) parts.push(`Avg: ${task.average.toFixed(1)}`);
  if (task.median != null) parts.push(`Med: ${task.median}`);
  if (task.finalEstimate != null) parts.push(`Final: ${task.finalEstimate}`);

  if (parts.length > 0) {
    lines.push(`> ${parts.join(' | ')}`);
    lines.push('');
  }
}

export function formatResultsAsMarkdown(
  roomTitle: string,
  tasks: TaskEstimate[],
  participants: string[],
): string {
  const lines: string[] = [`# Results: ${roomTitle}`, ''];

  if (tasks.length === 0) {
    return lines.join('\n').trimEnd() + '\n';
  }

  for (const task of tasks) renderTask(task, participants, lines);

  const agreed = tasks.filter((t) => isConsensus(t));
  const totalSp = tasks.reduce((sum, t) => sum + getTaskSp(t), 0);
  lines.push('---');
  lines.push(`**Summary:** ${tasks.length} tasks | ${totalSp} SP | Consensus: ${agreed.length}/${tasks.length}`);

  return lines.join('\n').trimEnd() + '\n';
}
